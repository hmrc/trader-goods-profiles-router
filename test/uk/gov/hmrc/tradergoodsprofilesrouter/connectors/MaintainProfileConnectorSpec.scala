/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.tradergoodsprofilesrouter.connectors

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, verify, when}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.MaintainProfileEisRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.MaintainProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{BaseConnectorSpec, BaseMetricsSpec}

import java.time.Instant
import scala.concurrent.Future

class MaintainProfileConnectorSpec extends BaseConnectorSpec with BaseMetricsSpec {

  private val correlationId: String = "3e8dae97-b586-4cef-8511-68ac12da9028"
  private val timestamp             = Instant.parse("2024-05-12T12:15:15.456321Z")

  private val connector = new MaintainProfileConnector(appConfig, httpClientV2, dateTimeService, metricsRegistry)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder, metricsRegistry, timerContext)

    setUpAppConfig()
    setUpMetrics()
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.put(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any, any, any, any, any, any, any)).thenReturn(requestBuilder)
  }

  "maintain Profile" should {
    "return a 200 ok if EIS successfully maintain a profile and correct URL is used" in {
      when(requestBuilder.execute[Either[Result, MaintainProfileResponse]](any, any))
        .thenReturn(Future.successful(Right(maintainProfileResponse)))

      val result =
        await(connector.maintainProfile(maintainProfileEisRequest.as[MaintainProfileEisRequest], correlationId))

      val expectedUrl = s"http://localhost:1234/tgp/maintainprofile/v1"
      verify(httpClientV2).put(url"$expectedUrl")
      verify(requestBuilder).setHeader(expectedHeader(correlationId, "dummyMaintainProfileBearerToken", "PUT"): _*)
      verify(requestBuilder).withBody(maintainProfileEisRequest)
      verify(requestBuilder).execute(any, any)
      verifyExecuteWithParams(correlationId)

      result.value mustBe maintainProfileResponse
      withClue("process the response within a timer") {
        verifyMetrics("tgp.maintainprofile.connector")
      }
    }

    "return an error if EIS returns one" in {
      when(requestBuilder.execute[Either[Result, MaintainProfileResponse]](any, any))
        .thenReturn(Future.successful(Left(BadRequest("error"))))

      val result =
        await(connector.maintainProfile(maintainProfileEisRequest.as[MaintainProfileEisRequest], correlationId))

      result.left.value mustBe BadRequest("error")
    }
  }

  val maintainProfileEisRequest: JsValue =
    Json.parse("""
        |{
        |"eori": "GB123456789012",
        |"actorId":"GB098765432112",
        |"ukimsNumber":"XIUKIM47699357400020231115081800",
        |"nirmsNumber":"RMS-GB-123456",
        |"niphlNumber": "6S123456"
        |}
        |""".stripMargin)

  val maintainProfileResponse: MaintainProfileResponse =
    Json
      .parse("""
          |{
          |"eori": "GB123456789012",
          |"actorId":"GB098765432112",
          |"ukimsNumber":"XIUKIM47699357400020231115081800",
          |"nirmsNumber":"RMS-GB-123456",
          |"niphlNumber": "6S123456"
          |}
          |""".stripMargin)
      .as[MaintainProfileResponse]
}
