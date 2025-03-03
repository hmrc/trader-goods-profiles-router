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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.writeableOf_JsValue
import play.api.mvc.Result
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.HttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.payloads.UpdateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordEisResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{BaseConnectorSpec, CreateRecordDataSupport}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class UpdateRecordConnectorSpec extends BaseConnectorSpec with CreateRecordDataSupport {

  private val timestamp             = Instant.parse("2024-05-12T12:15:15.456321Z")
  private val correlationId: String = "3e8dae97-b586-4cef-8511-68ac12da9028"
  private val expectedResponse      = createOrUpdateRecordEisResponse
  private val eisConnector          = new UpdateRecordConnector(appConfig, httpClientV2, dateTimeService, as, config)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder)

    setUpAppConfig()
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.get(any)(any)).thenReturn(requestBuilder)
    when(httpClientV2.post(any)(any)).thenReturn(requestBuilder)
    when(httpClientV2.put(any)(any)).thenReturn(requestBuilder)
    when(httpClientV2.patch(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
    when(appConfig.useEisPatchMethod).thenReturn(true)
    when(requestBuilder.execute[Either[EisHttpErrorResponse, CreateOrUpdateRecordEisResponse]](any, any))
      .thenReturn(Future.successful(Right(expectedResponse)))
  }

  "patch" should {
    "update a record successfully using the patch method" in {
      when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)

      val request: UpdateRecordPayload = updateRecordPayload.as[UpdateRecordPayload]
      val result                       = await(eisConnector.patch(request, correlationId))

      result.value mustBe expectedResponse
      verify(httpClientV2).patch(url"http://localhost:1234/tgp/updaterecord/v1")
      verify(httpClientV2, never()).put(any)(any)
    }

    "return an error if EIS return an error" in {
      when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)
      when(requestBuilder.execute[Either[EisHttpErrorResponse, CreateOrUpdateRecordEisResponse]](any, any))
        .thenReturn(Future.successful(Left(badRequestEISError)))

      val request = updateRecordPayload.as[UpdateRecordPayload]
      val result  = await(eisConnector.patch(request, correlationId))

      result.left.value mustBe badRequestEISError
    }

    "send a request with the right url" in {
      when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)

      await(eisConnector.patch(updateRecordPayload.as[UpdateRecordPayload], correlationId))

      val expectedUrl = s"http://localhost:1234/tgp/updaterecord/v1"
      verify(httpClientV2).patch(url"$expectedUrl")
      verify(httpClientV2, never()).put(any)(any)
      verify(requestBuilder).setHeader(
        expectedHeaderWithAcceptAndContentTypeHeader(correlationId, "dummyRecordUpdateBearerToken"): _*
      )
      verify(requestBuilder).withBody(updateRecordPayload)
    }

    "call the PUT method when isPatchMethodEnabled is false" in {
      when(appConfig.useEisPatchMethod).thenReturn(false)
      when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)

      await(eisConnector.patch(updateRecordPayload.as[UpdateRecordPayload], correlationId))

      verify(httpClientV2).put(url"http://localhost:1234/tgp/updaterecord/v1")
      verify(httpClientV2, never()).patch(any)(any)
      verify(requestBuilder).setHeader(
        expectedHeaderWithAcceptAndContentTypeHeader(correlationId, "dummyRecordUpdateBearerToken"): _*
      )
    }

    "add the clientID when calling the PUT method" in {
      when(appConfig.useEisPatchMethod).thenReturn(false)
      when(appConfig.sendClientId).thenReturn(true)
      when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)

      await(eisConnector.patch(updateRecordPayload.as[UpdateRecordPayload], correlationId))

      verify(requestBuilder).setHeader(
        expectedHeader(correlationId, "dummyRecordUpdateBearerToken"): _*
      )
    }
  }

  "put" should {

    "update a record successfully" in {
      when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)

      val request = updateRecordPayload.as[UpdateRecordPayload]
      val result  = await(eisConnector.put(request, correlationId))

      result.value mustBe expectedResponse
    }

    "return an error if EIS return an error" in {
      when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[Either[EisHttpErrorResponse, CreateOrUpdateRecordEisResponse]](any, any))
        .thenReturn(Future.successful(Left(badRequestEISError)))

      val request = updateRecordPayload.as[UpdateRecordPayload]
      val result  = await(eisConnector.put(request, correlationId))

      result.left.value mustBe badRequestEISError
    }

    "send a request with the right url without clientID" in {
      when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)
      when(
        requestBuilder
          .execute(any[HttpReader[Either[Result, CreateOrUpdateRecordEisResponse]]](), any[ExecutionContext]())
      )
        .thenReturn(Future.successful(Right(expectedResponse)))

      await(eisConnector.put(updateRecordPayload.as[UpdateRecordPayload], correlationId))

      val expectedUrl = s"http://localhost:1234/tgp/puttgprecord/v1"
      verify(httpClientV2).put(eqTo(url"$expectedUrl"))(any[HeaderCarrier]())
      verify(requestBuilder)
        .execute(any[HttpReader[Either[Result, CreateOrUpdateRecordEisResponse]]](), any[ExecutionContext]())
    }

  }

  private lazy val updateRecordPayload: JsValue = Json
    .parse("""
             |{
             |    "eori": "GB123456789001",
             |    "actorId": "GB098765432112",
             |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
             |    "traderRef": "BAN001001",
             |    "comcode": "10410100",
             |    "goodsDescription": "Organic bananas",
             |    "countryOfOrigin": "EC",
             |    "category": 1,
             |    "assessments": [
             |        {
             |            "assessmentId": "abc123",
             |            "primaryCategory": 1,
             |            "condition": {
             |                "type": "abc123",
             |                "conditionId": "Y923",
             |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
             |                "conditionTraderText": "Excluded product"
             |            }
             |        }
             |    ],
             |    "supplementaryUnit": 500,
             |    "measurementUnit": "Square metre (m2)",
             |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
             |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
             |}
             |""".stripMargin)
}
