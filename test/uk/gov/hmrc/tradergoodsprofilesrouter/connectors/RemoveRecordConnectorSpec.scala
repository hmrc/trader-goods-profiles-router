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
import org.mockito.captor.ArgCaptor
import play.api.http.Status.OK
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.LegacyStatusHttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.support.BaseConnectorSpec

import java.time.Instant
import scala.concurrent.Future

class RemoveRecordConnectorSpec extends BaseConnectorSpec {

  private val eori                  = "GB123456789011"
  private val actorId               = "GB123456789011"
  private val recordId              = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  private val timestamp             = Instant.parse("2024-05-12T12:15:15.456321Z")
  private val correlationId: String = "3e8dae97-b586-4cef-8511-68ac12da9028"

  private val connector = new RemoveRecordConnector(appConfig, httpClientV2, dateTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder)

    setUpAppConfig()
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.put(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any, any, any, any, any, any, any)).thenReturn(requestBuilder)
  }

  "remove a record successfully" in {
    when(requestBuilder.execute[Either[Result, Int]](any, any))
      .thenReturn(Future.successful(Right(OK)))

    val result = await(connector.removeRecord(eori, recordId, actorId, correlationId))

    result.value mustBe OK
  }

  "send a request with the right url for remove record" in {
    when(requestBuilder.execute[Either[Result, Int]](any, any))
      .thenReturn(Future.successful(Right(OK)))

    val result =
      await(connector.removeRecord(eori, recordId, actorId, correlationId))

    val expectedUrl = s"http://localhost:1234/tgp/removerecord/v1"
    verify(httpClientV2).put(url"$expectedUrl")
    verify(requestBuilder).setHeader(expectedHeader(correlationId, "dummyRecordRemoveBearerToken"): _*)
    verify(requestBuilder)
      .withBody(Json.obj("eori" -> eori, "recordId" -> recordId, "actorId" -> actorId).as[JsValue])
    verifyExecuteWithParamsType(correlationId)

    result.value mustBe OK
  }

  "return an error if EIS return an error" in {
    when(requestBuilder.execute[Either[Result, Int]](any, any))
      .thenReturn(Future.successful(Left(BadRequest("error"))))

    val result = await(connector.removeRecord(eori, recordId, actorId, correlationId))

    result.left.value mustBe BadRequest("error")
  }

  private def verifyExecuteWithParamsType(expectedCorrelationId: String) = {
    val captor = ArgCaptor[LegacyStatusHttpReader]
    verify(requestBuilder).execute(captor.capture, any)

    val httpReader = captor.value
    httpReader.correlationId mustBe expectedCorrelationId
  }

}
