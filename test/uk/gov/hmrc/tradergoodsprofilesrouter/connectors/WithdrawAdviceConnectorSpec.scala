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
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.MimeTypes
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService
import uk.gov.hmrc.tradergoodsprofilesrouter.support.BaseConnectorSpec

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

class WithdrawAdviceConnectorSpec extends BaseConnectorSpec with BeforeAndAfterEach {

  private val uuidService   = mock[UuidService]
  private val correlationId = UUID.randomUUID().toString

  private val withdrawDate             = Instant.parse("2024-05-12T12:15:15.678Z")
  private val recordId                 = "recordId"
  private val withdrawReason           = "Withdraw Reason"
  private val withdrawReasonWithSpaces = "  Withdraw Reason  "

  val sut = new WithdrawAdviceConnector(appConfig, httpClientV2, uuidService, dateTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder)

    setUpAppConfig()
    when(uuidService.uuid).thenReturn(correlationId)
    when(dateTimeService.timestamp).thenReturn(withdrawDate)
    when(httpClientV2.put(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any, any, any, any, any, any)).thenReturn(requestBuilder)
  }
  "put" should {
    "return 204" in {
      when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any, any))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      val result = await(sut.put(recordId, Some(withdrawReason)))

      result.value mustBe NO_CONTENT

      withClue("should send request to EIS with the right parameters") {
        val expectedUrl = s"http://localhost:1234/tgp/withdrawaccreditation/v1"
        verify(httpClientV2).put(url"$expectedUrl")
        verify(requestBuilder).setHeader(expectedHeader: _*)
        verify(requestBuilder).withBody(createExpectedPayload)
        verifyExecuteForStatusHttpReader(correlationId)
      }
    }

    "send a request without withdrawReason when not specified" in {
      when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any, any))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      val result = await(sut.put(recordId, None))

      result.value mustBe NO_CONTENT
      verify(requestBuilder).withBody(createExpectedPayloadWithoutWithdrawReason)
    }

    "send a request without withdrawReason when reason is only empty spaces" in {
      when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any, any))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      val result = await(sut.put(recordId, Some(" ")))

      result.value mustBe NO_CONTENT

      withClue("should send request to EIS with the right parameters") {
        val expectedUrl = s"http://localhost:1234/tgp/withdrawaccreditation/v1"
        verify(httpClientV2).put(url"$expectedUrl")
        verify(requestBuilder).setHeader(expectedHeader: _*)
        verify(requestBuilder).withBody(createExpectedPayloadWithoutWithdrawReason)
        verifyExecuteForStatusHttpReader(correlationId)
      }
    }

    "send a request with trimmed withdrawReason" in {
      when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any, any))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      val result = await(sut.put(recordId, Some(withdrawReasonWithSpaces)))

      result.value mustBe NO_CONTENT

      withClue("should send request to EIS with the right parameters") {
        val expectedUrl = s"http://localhost:1234/tgp/withdrawaccreditation/v1"
        verify(httpClientV2).put(url"$expectedUrl")
        verify(requestBuilder).setHeader(expectedHeader: _*)
        verify(requestBuilder).withBody(createExpectedPayload)
        verifyExecuteForStatusHttpReader(correlationId)
      }
    }

    "return an error when EIS httpclient return an error" in {
      val errorResponse = EisHttpErrorResponse(400, ErrorResponse(correlationId, "code", "message"))

      when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any, any))
        .thenReturn(Future.successful(Left(errorResponse)))

      val result = await(sut.put(recordId, Some(withdrawReason)))

      result.left.value mustBe errorResponse
    }

    "return an error if client api throw" in {
      when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any, any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(sut.put(recordId, Some(withdrawReason)))

      result.left.value mustBe EisHttpErrorResponse(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error")
      )
    }
  }

  private val expectedHeader: Seq[(String, String)] =
    Seq(
      "X-Correlation-ID" -> correlationId,
      "X-Forwarded-Host" -> "MDTP",
      "Date"             -> "Sun, 12 May 2024 12:15:15 GMT",
      "Authorization"    -> "Bearer dummyWithdrawAdviceBearerToken",
      "Accept"           -> MimeTypes.JSON,
      "Content-Type"     -> MimeTypes.JSON
    )

  private def createExpectedPayload: JsValue =
    Json.parse(s"""{
                 |   "withdrawRequest":{
                 |      "requestCommon": {
                 |        "clientID": "TSS"
                 |      },
                 |      "requestDetail":{
                 |         "withdrawDetail":{
                 |            "withdrawDate":"2024-05-12T12:15:15Z",
                 |            "withdrawReason": "$withdrawReason"
                 |         },
                 |         "goodsItems":[
                 |            {
                 |               "publicRecordID":"$recordId"
                 |            }
                 |         ]
                 |      }
                 |   }
                 |}""".stripMargin)

  private def createExpectedPayloadWithoutWithdrawReason: JsValue =
    Json.parse(s"""{
                  |   "withdrawRequest":{
                  |      "requestCommon": {
                  |        "clientID": "TSS"
                  |      },
                  |      "requestDetail":{
                  |         "withdrawDetail":{
                  |            "withdrawDate":"2024-05-12T12:15:15Z"
                  |         },
                  |         "goodsItems":[
                  |            {
                  |               "publicRecordID":"$recordId"
                  |            }
                  |         ]
                  |      }
                  |   }
                  |}""".stripMargin)
}
