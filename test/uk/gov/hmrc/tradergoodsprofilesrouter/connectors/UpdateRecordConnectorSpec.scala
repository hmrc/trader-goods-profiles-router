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
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.payloads.UpdateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{BaseConnectorSpec, CreateRecordDataSupport, MetricsSupportSpec}

import java.time.Instant
import scala.concurrent.Future

class UpdateRecordConnectorSpec extends BaseConnectorSpec with MetricsSupportSpec with CreateRecordDataSupport {

  private val timestamp             = Instant.parse("2024-05-12T12:15:15.456321Z")
  private val correlationId: String = "3e8dae97-b586-4cef-8511-68ac12da9028"
  private val eisConnector          = new UpdateRecordConnector(appConfig, httpClientV2, dateTimeService, metricsRegistry)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder, metricsRegistry, timerContext)

    setUpAppConfig()
    setUpMetrics()
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.get(any)(any)).thenReturn(requestBuilder)
    when(httpClientV2.post(any)(any)).thenReturn(requestBuilder)
    when(httpClientV2.put(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any, any, any, any, any, any, any)).thenReturn(requestBuilder)
  }

  "updateRecord" should {
    "update a record successfully" in {
      val expectedResponse: CreateOrUpdateRecordResponse =
        createOrUpdateRecordSampleJson.as[CreateOrUpdateRecordResponse]

      when(requestBuilder.execute[Either[Result, CreateOrUpdateRecordResponse]](any, any))
        .thenReturn(Future.successful(Right(expectedResponse)))

      val request = updateRecordPayload.as[UpdateRecordPayload]
      val result  = await(eisConnector.updateRecord(request, correlationId))

      result.value mustBe expectedResponse

      withClue("process the response within a timer") {
        verifyMetrics("tgp.updaterecord.connector")
      }
    }

    "return an error if EIS return an error" in {
      when(requestBuilder.execute[Either[Result, CreateOrUpdateRecordResponse]](any, any))
        .thenReturn(Future.successful(Left(BadRequest("error"))))

      val request = updateRecordPayload.as[UpdateRecordPayload]
      val result  = await(eisConnector.updateRecord(request, correlationId))

      result.left.value mustBe BadRequest("error")
    }

    "send a request with the right url" in {

      val expectedResponse: CreateOrUpdateRecordResponse =
        createOrUpdateRecordSampleJson.as[CreateOrUpdateRecordResponse]
      when(requestBuilder.execute[Either[Result, CreateOrUpdateRecordResponse]](any, any))
        .thenReturn(Future.successful(Right(expectedResponse)))

      await(eisConnector.updateRecord(updateRecordPayload.as[UpdateRecordPayload], correlationId))

      val expectedUrl = s"http://localhost:1234/tgp/updaterecord/v1"
      verify(httpClientV2).put(url"$expectedUrl")
      verify(requestBuilder).setHeader(expectedHeader(correlationId, "dummyRecordUpdateBearerToken"): _*)
      verify(requestBuilder).withBody(updateRecordPayload)

      verifyExecuteWithParams(correlationId)
    }
  }

  val updateRecordPayload: JsValue = Json
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
