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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import play.api.http.MimeTypes
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.StatusHttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.advicerequests.TraderDetails
import uk.gov.hmrc.tradergoodsprofilesrouter.support.BaseConnectorSpec

import java.time.Instant
import scala.concurrent.Future

class AccreditationConnectorSpec extends BaseConnectorSpec {

  private val timestamp             = Instant.parse("2024-05-12T12:15:15.456321Z")
  private val correlationId: String = "3e8dae97-b586-4cef-8511-68ac12da9028"

  private val sut: RequestAdviceConnector =
    new RequestAdviceConnector(appConfig, httpClientV2, dateTimeService, as, config)

  private val expectedHeader: Seq[(String, String)] =
    Seq(
      "X-Correlation-ID" -> correlationId,
      "X-Forwarded-Host" -> "MDTP",
      "Date"             -> "Sun, 12 May 2024 12:15:15 GMT",
      "Authorization"    -> "Bearer dummyAccreditationCreateBearerToken",
      "Accept"           -> MimeTypes.JSON,
      "Content-Type"     -> MimeTypes.JSON
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder)

    setUpAppConfig()
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.post(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)

  }

  "request Advice" should {
    "return 200 OK" in {
      when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any, any))
        .thenReturn(Future.successful(Right(200)))

      val traderDetails = TraderDetails("eori", "any-name", None, "sample@sample.com", "ukims", Seq.empty)

      val result = await(sut.requestAdvice(traderDetails, correlationId))

      result.value mustBe OK
    }

    "send a request to EIS with the right parameters" in {
      when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any, any))
        .thenReturn(Future.successful(Right(200)))

      val traderDetails = TraderDetails("eori", "any-name", None, "sample@sample.com", "ukims", Seq.empty)

      await(sut.requestAdvice(traderDetails, correlationId))

      val expectedUrl = s"http://localhost:1234/tgp/createaccreditation/v1"
      verify(httpClientV2).post(url"$expectedUrl")
      verify(requestBuilder).setHeader(expectedHeader: _*)
      verify(requestBuilder).withBody(expectedJsonBody)
      verifyExecuteHttpRequest(correlationId)
    }

    "return an error" in {
      when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any, any))
        .thenReturn(Future.successful(Left(badRequestEISError)))

      val traderDetails = TraderDetails("eori", "any-name", None, "sample@sample.com", "ukims", Seq.empty)

      val result = await(sut.requestAdvice(traderDetails, correlationId))

      result.left.value mustBe badRequestEISError
    }
  }

  private def expectedJsonBody =
    Json.parse("""
        |{
        |"accreditationRequest":{
        | "requestCommon":{
        |   "receiptDate":"2024-05-12T12:15:15Z"
        |   },
        |   "requestDetail":{
        |     "traderDetails":{
        |       "traderEORI":"eori",
        |       "requestorName":"any-name",
        |       "requestorEmail":"sample@sample.com",
        |       "ukimsAuthorisation":"ukims",
        |       "goodsItems":[]
        |       }
        |   }
        | }
        |}
        |""".stripMargin)

  private def verifyExecuteHttpRequest(expectedCorrelationId: String) = {
    val captor = ArgumentCaptor.forClass(classOf[StatusHttpReader])
    verify(requestBuilder).execute(captor.capture, any)

    val httpReader = captor.getValue
    httpReader.correlationId mustBe expectedCorrelationId
  }
}
