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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mockito.captor.ArgCaptor
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.HttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.UpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.MaintainProfileEisRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{GetEisRecordsResponse, MaintainProfileResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.support.BaseConnectorSpec
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames.ClientId

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class EISConnectorSpec extends BaseConnectorSpec {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders = Seq((ClientId, "TSS")))

  private val dateTimeService: DateTimeService = mock[DateTimeService]
  private val timestamp                        = Instant.parse("2024-05-12T12:15:15.456321Z")
  private val eori                             = "GB123456789011"
  private val actorId                          = "GB123456789011"
  private val recordId                         = "12345"
  implicit val correlationId: String           = "3e8dae97-b586-4cef-8511-68ac12da9028"

  private val eisConnector: EISConnector = new EISConnector(appConfig, httpClientV2, dateTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder)

    setUpAppConfig()
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.get(any)(any)).thenReturn(requestBuilder)
    when(httpClientV2.post(any)(any)).thenReturn(requestBuilder)
    when(httpClientV2.put(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any, any, any, any, any, any, any)).thenReturn(requestBuilder)
  }

  "fetchRecord" should {
    "fetch a record successfully" in {
      val response: GetEisRecordsResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]

      when(requestBuilder.execute[Either[Result, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      val result = await(eisConnector.fetchRecord(eori, recordId, correlationId))

      result.value mustBe response
    }

    "return an error whenEIS return an error" in {
      when(requestBuilder.execute[Either[Result, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Left(BadRequest("error"))))

      val result = await(eisConnector.fetchRecord(eori, recordId, correlationId))

      result.left.value mustBe BadRequest("error")
    }

    "send a request with the right parameters" in {
      val response: GetEisRecordsResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]

      when(requestBuilder.execute[Any](any, any))
        .thenReturn(Future.successful(Right(response)))

      await(eisConnector.fetchRecord(eori, recordId, correlationId))
      val expectedUrl = s"http://localhost:1234/tgp/getrecords/v1/$eori/$recordId"
      verify(httpClientV2).get(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(buildHeaders(correlationId, "dummyRecordGetBearerToken"): _*)

      verifyExecuteWithParams(correlationId)
    }
  }

  "fetchRecords" should {
    "fetch multiple records successfully" in {
      val response: GetEisRecordsResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]

      when(requestBuilder.execute[Either[Result, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      val result = await(eisConnector.fetchRecords(eori, correlationId))

      result.value mustBe response
    }

    "return an error if EIS return an error" in {
      when(requestBuilder.execute[Either[Result, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Left(BadRequest("error"))))

      val result = await(eisConnector.fetchRecord(eori, recordId, correlationId))

      result.left.value mustBe BadRequest("error")
    }

    "send a request with the right url for fetch records" in {
      val response: GetEisRecordsResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]

      when(requestBuilder.execute[Either[Result, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      await(eisConnector.fetchRecords(eori, correlationId, Some(timestamp), Some(1), Some(1)))

      val expectedLastUpdateDate = Instant.parse("2024-05-12T12:15:15Z")
      val expectedUrl            =
        s"http://localhost:1234/tgp/getrecords/v1/$eori?lastUpdatedDate=$expectedLastUpdateDate&page=1&size=1"
      verify(httpClientV2).get(url"$expectedUrl")
      verify(requestBuilder).setHeader(buildHeaders(correlationId, "dummyRecordGetBearerToken"): _*)
      verifyExecuteWithParams(correlationId)

    }
  }

  "updateRecord" should {
    "update a record successfully" in {
      val expectedResponse: CreateOrUpdateRecordResponse =
        createOrUpdateRecordSampleJson.as[CreateOrUpdateRecordResponse]

      when(requestBuilder.execute[Either[Result, CreateOrUpdateRecordResponse]](any, any))
        .thenReturn(Future.successful(Right(expectedResponse)))

      val request: UpdateRecordRequest = updateRecordRequest.as[UpdateRecordRequest]
      val result                       = await(eisConnector.updateRecord(request, correlationId))

      result.value mustBe expectedResponse
    }

    "return an error if EIS return an error" in {
      when(requestBuilder.execute[Either[Result, CreateOrUpdateRecordResponse]](any, any))
        .thenReturn(Future.successful(Left(BadRequest("error"))))

      val request: UpdateRecordRequest = updateRecordRequest.as[UpdateRecordRequest]
      val result                       = await(eisConnector.updateRecord(request, correlationId))

      result.left.value mustBe BadRequest("error")
    }

    "send a request with the right url" in {

      val expectedResponse: CreateOrUpdateRecordResponse =
        createOrUpdateRecordSampleJson.as[CreateOrUpdateRecordResponse]
      when(requestBuilder.execute[Either[Result, CreateOrUpdateRecordResponse]](any, any))
        .thenReturn(Future.successful(Right(expectedResponse)))

      await(eisConnector.updateRecord(updateRecordRequest.as[UpdateRecordRequest], correlationId))

      val expectedUrl = s"http://localhost:1234/tgp/updaterecord/v1"
      verify(httpClientV2).put(url"$expectedUrl")
      verify(requestBuilder).setHeader(buildHeaders(correlationId, "dummyRecordUpdateBearerToken"): _*)
      verify(requestBuilder).withBody(updateRecordRequest)
      verify(requestBuilder).execute(any, any)

      verifyExecuteWithParams(correlationId)
    }
  }

  "maintain Profile" should {
    "return a 200 ok if EIS successfully maintain a profile and correct URL is used" in {
      when(requestBuilder.execute[Either[Result, MaintainProfileResponse]](any, any))
        .thenReturn(Future.successful(Right(maintainProfileResponse)))

      val result =
        await(eisConnector.maintainProfile(maintainProfileEisRequest.as[MaintainProfileEisRequest], correlationId))

      val expectedUrl = s"http://localhost:1234/tgp/maintainprofile/v1"
      verify(httpClientV2).put(url"$expectedUrl")
      verify(requestBuilder).setHeader(buildHeaders(correlationId, "dummyMaintainProfileBearerToken"): _*)
      verify(requestBuilder).withBody(maintainProfileEisRequest)
      verify(requestBuilder).execute(any, any)
      verifyExecuteWithParams

      result.value mustBe maintainProfileResponse
    }

    "return an error if EIS returns one" in {
      when(requestBuilder.execute[Either[Result, MaintainProfileResponse]](any, any))
        .thenReturn(Future.successful(Left(BadRequest("error"))))

      val result =
        await(eisConnector.maintainProfile(maintainProfileEisRequest.as[MaintainProfileEisRequest], correlationId))

      result.left.value mustBe BadRequest("error")
    }

  }

  private def verifyExecuteWithParams = {
    val captor = ArgCaptor[HttpReads[Either[Result, GetEisRecordsResponse]]]
    verify(requestBuilder).execute(captor.capture, any)

    val httpReader = captor.value
    httpReader.asInstanceOf[HttpReader[Either[Result, Any]]].correlationId mustBe correlationId
  }

  lazy val createOrUpdateRecordSampleJson: JsValue = Json
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
        |  "createdDateTime": "2024-11-18T23:20:19Z",
        |  "updatedDateTime": "2024-11-18T23:20:19Z",
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

  val updateRecordRequest: JsValue = Json
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

  val maintainProfileEisRequest: JsValue =
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
