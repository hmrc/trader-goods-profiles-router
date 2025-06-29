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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import play.api.libs.ws.writeableOf_JsValue
import play.api.mvc.Result
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.HttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.CreateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordEisResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{BaseConnectorSpec, CreateRecordDataSupport}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class CreateRecordConnectorSpec extends BaseConnectorSpec with CreateRecordDataSupport {

  private val timestamp              = Instant.parse("2024-05-12T12:15:15.456321Z")
  implicit val correlationId: String = "3e8dae97-b586-4cef-8511-68ac12da9028"

  private val connector = new CreateRecordConnector(appConfig, httpClientV2, dateTimeService, as, config)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder)

    setUpAppConfig()
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.post(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(appConfig.sendClientId).thenReturn(true)
  }

  "createRecord" should {
    "create a record successfully" in {
      val expectedResponse = createOrUpdateRecordEisResponse

      when(requestBuilder.execute[Either[EisHttpErrorResponse, CreateOrUpdateRecordEisResponse]](any, any))
        .thenReturn(Future.successful(Right(expectedResponse)))

      val request = createRecordEisPayload.as[CreateRecordPayload]
      val result  = await(connector.createRecord(request, correlationId))

      result.value mustBe expectedResponse
    }

    "return an error if EIS return an error" in {
      when(requestBuilder.execute[Either[EisHttpErrorResponse, CreateOrUpdateRecordEisResponse]](any, any))
        .thenReturn(Future.successful(Left(badRequestEISError)))

      val request = createRecordEisPayload.as[CreateRecordPayload]
      val result  = await(connector.createRecord(request, correlationId))

      result.left.value mustBe badRequestEISError
    }

    "send a request with the right url" when {
      "sendClientId feature flag is false" in {
        when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
        val expectedResponse = createOrUpdateRecordEisResponse
        when(requestBuilder.execute[Either[EisHttpErrorResponse, CreateOrUpdateRecordEisResponse]](any, any))
          .thenReturn(Future.successful(Right(expectedResponse)))
        when(appConfig.sendClientId).thenReturn(false)

        await(connector.createRecord(createRecordEisPayload.as[CreateRecordPayload], correlationId))

        val expectedUrl = s"http://localhost:1234/tgp/createrecord/v1"
        verify(httpClientV2).post(url"$expectedUrl")
        verify(requestBuilder).setHeader(
          expectedHeaderWithAcceptAndContentTypeHeader(correlationId, "dummyRecordCreateBearerToken"): _*
        )
        verify(requestBuilder).withBody(createRecordEisPayload)
        verify(requestBuilder)
          .execute(any[HttpReader[Either[Result, CreateOrUpdateRecordEisResponse]]], any[ExecutionContext])
      }

      "sendClientId feature flag is true" in {
        when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[Either[EisHttpErrorResponse, CreateOrUpdateRecordEisResponse]](any, any))
          .thenReturn(Future.successful(Right(createOrUpdateRecordEisResponse)))

        await(connector.createRecord(createRecordEisPayload.as[CreateRecordPayload], correlationId))

        val expectedUrl = s"http://localhost:1234/tgp/createrecord/v1"
        verify(httpClientV2).post(url"$expectedUrl")

        val expectedHeaderWithClientId =
          expectedHeaderWithAcceptAndContentTypeHeader(
            correlationId,
            "dummyRecordCreateBearerToken"
          ) :+ ("X-Client-ID" -> "TSS")
        verify(requestBuilder).setHeader(expectedHeaderWithClientId: _*)

        verify(requestBuilder).setHeader(expectedHeaderWithClientId: _*)
        verify(requestBuilder).withBody(createRecordEisPayload)
        verify(requestBuilder)
          .execute(any[HttpReader[Either[Result, CreateOrUpdateRecordEisResponse]]], any[ExecutionContext])
      }
    }
  }

}
