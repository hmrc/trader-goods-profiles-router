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
import org.mockito.Mockito._
import org.mockito.MockitoSugar.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.{AppConfig, EISInstanceConfig, Headers}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames.ClientId

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class EISConnectorSpec extends AsyncFlatSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders = Seq((ClientId, "TSS")))

  private val appConfig: AppConfig             = mock[AppConfig]
  private val httpClientV2: HttpClientV2       = mock[HttpClientV2]
  private val requestBuilder                   = mock[RequestBuilder]
  private val dateTimeService: DateTimeService = mock[DateTimeService]
  private val timestamp                        = Instant.parse("2024-05-12T12:15:15.456321Z")

  private val eisConnector: EISConnectorImpl = new EISConnectorImpl(appConfig, httpClientV2, dateTimeService)

  override def beforeEach(): Unit = {
    reset(appConfig, httpClientV2, dateTimeService)
    super.beforeEach()

    when(appConfig.eisConfig).thenReturn(
      new EISInstanceConfig(
        "http",
        "localhost",
        1234,
        "/tgp/getrecords/v1",
        Headers("bearerToken")
      )
    )
    when(httpClientV2.get(any())(any())).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)
  }

  it should "fetch a record successfully" in {
    val eori          = "GB123456789011"
    val recordId      = "12345"
    val correlationId = "3e8dae97-b586-4cef-8511-68ac12da9028"

    when(dateTimeService.timestamp).thenReturn(timestamp)

    val sampleJson: JsValue = Json
      .parse("""
               |{
               |    "goodsItemRecords": [
               |        {
               |            "eori": "GB1234567890",
               |            "actorId": "GB1234567890",
               |            "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
               |            "traderRef": "BAN001001",
               |            "comcode": "104101000",
               |            "accreditationRequest": "Not requested",
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

    val expectedResponse: GetEisRecordsResponse = sampleJson.as[GetEisRecordsResponse]
    val httpResponse                            = HttpResponse(200, sampleJson, Map.empty)

    when(requestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(httpResponse))

    val result = await(eisConnector.fetchRecord(eori, recordId, correlationId)(ec, hc))

    result shouldBe expectedResponse
  }
}
