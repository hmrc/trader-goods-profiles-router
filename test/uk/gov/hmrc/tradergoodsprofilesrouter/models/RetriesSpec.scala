/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.tradergoodsprofilesrouter.models

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import play.api.http.Status.BAD_GATEWAY
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{EisHttpErrorResponse, GetRecordsConnector}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{BaseConnectorSpec, GetRecordsDataSupport}

import java.time.{Duration, Instant}
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class RetriesSpec extends BaseConnectorSpec with ScalaFutures with MockitoSugar with GetRecordsDataSupport {

  private implicit val patience: PatienceConfig = PatienceConfig(timeout = 10.seconds, interval = 500.millis)

  object TestRetries extends Retries {
    override protected def actorSystem: ActorSystem = ActorSystem()
    override protected def configuration: Config    = ConfigFactory.parseString(
      """
        |http-verbs.retries.intervals = [10ms, 10ms, 10ms]
        |""".stripMargin
    )
  }

  val successfulResponse: Right[Nothing, String] = Right("Success")

  def retryCondition: PartialFunction[EisHttpErrorResponse, Boolean] = { case EisHttpErrorResponse(BAD_GATEWAY, _) =>
    true
  }

  "retryFor" should {

    "retry when BAD_GATEWAY error occurs" in {

      var attempt = 0

      def failingBlock: Future[Either[EisHttpErrorResponse, String]] = Future {
        attempt += 1
        if (attempt < 3) Left(badGatewayEISError) else successfulResponse
      }

      whenReady(TestRetries.retryFor("test")(retryCondition)(failingBlock)) { result =>
        result mustBe successfulResponse
        attempt mustBe 3
      }
    }

    "not retry when a non-retryable error occurs (e.g., FORBIDDEN)" in {
      var attempt = 0

      def failingBlock: Future[Either[EisHttpErrorResponse, String]] = Future {
        attempt += 1
        Left(forbiddenEISError)
      }

      whenReady(TestRetries.retryFor("test")(retryCondition)(failingBlock)) { result =>
        result mustBe Left(forbiddenEISError)
        attempt mustBe 1
      }
    }

    "return success immediately if the first attempt succeeds" in {
      var attempt = 0

      def successBlock: Future[Either[EisHttpErrorResponse, String]] = Future {
        attempt += 1
        successfulResponse
      }

      whenReady(TestRetries.retryFor("test")(retryCondition)(successBlock)) { result =>
        result mustBe successfulResponse
        attempt mustBe 1
      }
    }

    "retry only up to the configured intervals" in {
      var attempt = 0

      def failingBlock: Future[Either[EisHttpErrorResponse, String]] = Future {
        attempt += 1
        Left(badGatewayEISError)
      }

      val result = TestRetries.retryFor("test")(retryCondition)(failingBlock)

      result.futureValue(timeout(500.millis)) mustBe Left(badGatewayEISError)
      attempt mustBe 4
    }

  }

  "when connector fails" should {
    val connector = new GetRecordsConnector(appConfig, httpClientV2, dateTimeService, as, config)

    val eori                  = "GB123456789011"
    val recordId              = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
    val correlationId: String = "3e8dae97-b586-4cef-8511-68ac12da9028"

    "repeat until failure" in {

      connectorCallMocks()

      when(requestBuilder.execute[Either[EisHttpErrorResponse, GetEisRecordsResponse]](any(), any()))
        .thenReturn(
          Future.successful(Left(badGatewayEISError)),
          Future.successful(Left(badGatewayEISError)),
          Future.successful(Left(badRequestEISError))
        )

      val result =
        connector.fetchRecord(eori, recordId, correlationId, "http://localhost:1234/tgp/getrecords/v1").futureValue

      result.left.value mustBe badRequestEISError
    }

    "repeat until success" in {
      val response: GetEisRecordsResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]

      connectorCallMocks()

      when(requestBuilder.execute[Either[EisHttpErrorResponse, GetEisRecordsResponse]](any(), any()))
        .thenReturn(
          Future.successful(Left(badGatewayEISError)),
          Future.successful(Left(badGatewayEISError)),
          Future.successful(Right(response))
        )

      val result =
        connector.fetchRecord(eori, recordId, correlationId, "http://localhost:1234/tgp/getrecords/v1").futureValue

      result.value mustBe response
    }
  }

  private def connectorCallMocks(): Unit = {
    val retriesConfig = List(Duration.ofMillis(1), Duration.ofMillis(1), Duration.ofMillis(1)).asJava

    val timestamp = Instant.parse("2024-05-12T12:15:15.456321Z")

    setUpAppConfig()

    when(config.getDurationList("http-verbs.retries.intervals")).thenReturn(retriesConfig)

    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.get(any())(any())).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)
  }
}
