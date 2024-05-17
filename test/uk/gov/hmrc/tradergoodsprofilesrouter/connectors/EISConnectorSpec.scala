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

import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.MimeTypes
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.{AppConfig, EISInstanceConfig, Headers}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames.ClientId

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class EISConnectorSpec extends PlaySpec with BeforeAndAfterEach with EitherValues {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders = Seq((ClientId, "TSS")))

  private val appConfig: AppConfig             = mock[AppConfig]
  private val httpClientV2: HttpClientV2       = mock[HttpClientV2]
  private val requestBuilder: RequestBuilder   = mock[RequestBuilder]
  private val dateTimeService: DateTimeService = mock[DateTimeService]
  private val timestamp                        = Instant.parse("2024-05-12T12:15:15.456321Z")
  private val eori                             = "GB123456789011"
  private val recordId                         = "12345"
  implicit val correlationId: String           = "3e8dae97-b586-4cef-8511-68ac12da9028"

  private val eisConnector: EISConnectorImpl = new EISConnectorImpl(appConfig, httpClientV2, dateTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder)

    when(appConfig.eisConfig).thenReturn(
      new EISInstanceConfig(
        "http",
        "localhost",
        1234,
        "/tgp/getrecords/v1",
        "/tgp/createrecord/v1",
        "MDTP",
        Headers("bearerToken")
      )
    )
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.get(any())(any())).thenReturn(requestBuilder)
    when(httpClientV2.post(any())(any())).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any())(any(), any(), any())).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any, any, any, any, any, any, any)).thenReturn(requestBuilder)
  }

  "fetchRecord" should {
    "fetch a record successfully" in {
      val expectedResponse: GetEisRecordsResponse = sampleJson.as[GetEisRecordsResponse]
      val httpResponse                            = HttpResponse(200, sampleJson, Map.empty)
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(httpResponse))

      val result = await(eisConnector.fetchRecord(eori, recordId))

      result.value mustBe expectedResponse
    }

    "response json failure" in {
      when(requestBuilder.execute[HttpResponse](any, any))
        .thenReturn(Future.successful(HttpResponse(200, "message")))

      val exception = intercept[RuntimeException] {
        await(eisConnector.fetchRecord(eori, recordId))
      }

      exception.getMessage mustBe s"Response body could not be read: message"
    }

    "send a request with the right url" in {
      val headers = Seq(
        "X-Correlation-ID" -> correlationId,
        "X-Forwarded-Host" -> "MDTP",
        "Content-Type"     -> MimeTypes.JSON,
        "Accept"           -> MimeTypes.JSON,
        "Date"             -> "Sun, 12 May 2024 12:15:15 Z",
        "X-Client-ID"      -> "TSS",
        "Authorization"    -> "bearerToken"
      )

      val expectedResponse: GetEisRecordsResponse = sampleJson.as[GetEisRecordsResponse]
      val httpResponse                            = HttpResponse(200, sampleJson, Map.empty)
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(httpResponse))

      val result = await(eisConnector.fetchRecord(eori, recordId))

      val expectedUrl = s"http://localhost:1234/tgp/getrecords/v1/$eori/$recordId"
      verify(httpClientV2).get(url"$expectedUrl")
      verify(requestBuilder).setHeader(headers: _*)
      verify(requestBuilder).execute(any, any)

      result.value mustBe expectedResponse
    }
  }

  "fetchRecords" should {
    "fetch multiple records successfully" in {
      val expectedResponse: GetEisRecordsResponse = sampleJson.as[GetEisRecordsResponse]
      val httpResponse                            = HttpResponse(200, sampleJson, Map.empty)

      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(httpResponse))

      val result = await(eisConnector.fetchRecords(eori))

      result.value mustBe expectedResponse
    }

    "response json failure for fetch records" in {
      when(requestBuilder.execute[HttpResponse](any, any))
        .thenReturn(Future.successful(HttpResponse(200, "message")))

      val exception = intercept[RuntimeException] {
        await(eisConnector.fetchRecords(eori))
      }

      exception.getMessage mustBe s"Response body could not be read: message"
    }

    "send a request with the right url for fetch records" in {
      val headers = Seq(
        "X-Correlation-ID" -> correlationId,
        "X-Forwarded-Host" -> "MDTP",
        "Content-Type"     -> MimeTypes.JSON,
        "Accept"           -> MimeTypes.JSON,
        "Date"             -> "Sun, 12 May 2024 12:15:15 Z",
        "X-Client-ID"      -> "TSS",
        "Authorization"    -> "bearerToken"
      )

      val expectedResponse: GetEisRecordsResponse = sampleJson.as[GetEisRecordsResponse]
      val httpResponse                            = HttpResponse(200, sampleJson, Map.empty)
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(httpResponse))

      val result =
        await(eisConnector.fetchRecords(eori, Some(timestamp.toString), Some(1), Some(1)))

      val expectedUrl = s"http://localhost:1234/tgp/getrecords/v1/$eori?lastUpdatedDate=$timestamp&page=1&size=1"
      verify(httpClientV2).get(url"$expectedUrl")
      verify(requestBuilder).setHeader(headers: _*)
      verify(requestBuilder).execute(any, any)

      result.value mustBe expectedResponse
    }
  }

  val fetchRecordSampleJson: JsValue = Json
    .parse("""
             |{
             |    "goodsItemRecords": [
             |        {
             |            "eori": "GB1234567890",
             |            "actorId": "GB1234567890",
             |            "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
             |            "traderRef": "BAN001001",
             |            "comcode": "104101000",
             |            "accreditationStatus": "Not requested",
             |            "goodsDescription": "Organic bananas",
             |            "countryOfOrigin": "EC",
             |            "category": 3,
             |            "assessments": [
             |                {
             |                    "assessmentId": "abc123",
             |                    "primaryCategory": "1",
             |                    "condition": {
             |                        "type": "abc123",
             |                        "conditionId": "Y923",
             |                        "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
             |                        "conditionTraderText": "Excluded product"
             |                    }
             |                }
             |            ],
             |            "supplementaryUnit": 500,
             |            "measurementUnit": "square meters(m^2)",
             |            "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
             |            "comcodeEffectiveToDate": "",
             |            "version": 1,
             |            "active": true,
             |            "toReview": false,
             |            "reviewReason": null,
             |            "declarable": "IMMI declarable",
             |            "ukimsNumber": "XIUKIM47699357400020231115081800",
             |            "nirmsNumber": "RMS-GB-123456",
             |            "niphlNumber": "6 S12345",
             |            "locked": false,
             |            "srcSystemName": "CDAP",
             |            "createdDateTime": "2024-11-18T23:20:19Z",
             |            "updatedDateTime": "2024-11-18T23:20:19Z"
             |        }
             |    ],
             |    "pagination": {
             |        "totalRecords": 1,
             |        "currentPage": 0,
             |        "totalPages": 1,
             |        "nextPage": null,
             |        "prevPage": null
             |    }
             |}
             |""".stripMargin)

  val createRecordSampleJson: JsValue = Json
    .parse("""
        |{
        |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
        |  "eori": "GB123456789012",
        |  "actorId": "GB098765432112",
        |  "traderRef": "BAN001001",
        |  "comcode": "104101000",
        |  "accreditationStatus": "Not Requested",
        |  "goodsDescription": "Organic bananas",
        |  "countryOfOrigin": "EC",
        |  "category": 1,
        |  "supplementaryUnit": 500,
        |  "measurementUnit": "Square metre (m2)",
        |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
        |  "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
        |  "version": 1,
        |  "active": true,
        |  "toReview": false,
        |  "reviewReason": "Commodity code change",
        |  "declarable": "SPIMM",
        |  "ukimsNumber": "XIUKIM47699357400020231115081800",
        |  "nirmsNumber": "RMS-GB-123456",
        |  "niphlNumber": "6 S12345",
        |  "createdDateTime": "2024-11-18T23->20->19Z",
        |  "updatedDateTime": "2024-11-18T23->20->19Z",
        |  "assessments": [
        |    {
        |      "assessmentId": "abc123",
        |      "primaryCategory": 1,
        |      "condition": {
        |        "type": "abc123",
        |        "conditionId": "Y923",
        |        "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
        |        "conditionTraderText": "Excluded product"
        |      }
        |    }
        |  ]
        |}
        |""".stripMargin)

  val createRecordRequest: JsValue = Json
    .parse("""
        |{
        |    "eori": "GB123456789012",
        |    "actorId": "GB098765432112",
        |    "traderRef": "BAN001001",
        |    "comcode": "104101000",
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
