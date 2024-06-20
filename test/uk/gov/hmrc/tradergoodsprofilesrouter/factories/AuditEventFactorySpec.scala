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

package uk.gov.hmrc.tradergoodsprofilesrouter.factories

import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK}
import play.api.libs.json.{JsObject, Json, __}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.models.ResponseModelSupport.removeNulls
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.{CreateRecordRequest, UpdateRecordRequest}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService

import java.time.Instant

class AuditEventFactorySpec extends PlaySpec with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = Seq(("X-Client-ID", "TSS")))
  private val timestamp          = Instant.parse("2021-12-17T09:30:47.7896Z")
  private val eori               = "eori"
  private val recordId           = "recordId"
  private val actorId            = "actorId"
  private val dataTimeService    = mock[DateTimeService]
  private val sut                = new AuditEventFactory(dataTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(dataTimeService)
    when(dataTimeService.timestamp).thenReturn(timestamp)
  }

  "removeRecord" should {
    "return a ExtendedDataEvent with no failure reason" in {
      val result = sut.removeRecord(eori, recordId, actorId, timestamp.toString, "SUCCEEDED", NO_CONTENT)

      result.auditSource mustBe "trader-goods-profiles-router"
      result.auditType mustBe "ManageGoodsRecord"
      result.detail mustBe auditRemoveRecordDetailJson("SUCCEEDED", NO_CONTENT)
    }

    "return a ExtendedDataEvent with failure reason" in {
      val failureReason = Seq("error-1", "error-2")
      val result        =
        sut.removeRecord(
          eori,
          recordId,
          actorId,
          timestamp.toString,
          "BAD_REQUEST",
          BAD_REQUEST,
          Some(failureReason)
        )

      result.auditSource mustBe "trader-goods-profiles-router"
      result.auditType mustBe "ManageGoodsRecord"
      result.detail mustBe auditDetailJsonWithFailureReason(
        failureReason,
        auditRemoveRecordDetailJson("BAD_REQUEST", BAD_REQUEST)
      )
    }
  }

  "createRecord" should {
    "return a ExtendedDataEvent with no failure reason" in {

      val result = sut.createRecord(
        createRecordRequestData,
        timestamp.toString,
        "SUCCEEDED",
        OK,
        None,
        Some(createOrUpdateRecordResponseData)
      )

      result.auditSource mustBe "trader-goods-profiles-router"
      result.auditType mustBe "ManageGoodsRecord"
      result.detail mustBe auditCreateRecordDetailJson("SUCCEEDED", OK, Some(createOrUpdateRecordResponseData))
    }

    "return a ExtendedDataEvent with failure reason" in {
      val failureReason = Seq("error-1", "error-2")
      val result        = sut.createRecord(
        createRecordRequestData,
        timestamp.toString,
        "BAD_REQUEST",
        BAD_REQUEST,
        Some(failureReason)
      )

      result.auditSource mustBe "trader-goods-profiles-router"
      result.auditType mustBe "ManageGoodsRecord"
      result.detail mustBe auditDetailJsonWithFailureReason(
        failureReason,
        auditCreateRecordDetailJson("BAD_REQUEST", BAD_REQUEST)
      )
    }
  }

  "updateRecord" should {
    "return a ExtendedDataEvent with no failure reason" in {

      val result = sut.updateRecord(
        updateRecordRequestData,
        timestamp.toString,
        "SUCCEEDED",
        OK,
        None,
        Some(createOrUpdateRecordResponseData)
      )

      result.auditSource mustBe "trader-goods-profiles-router"
      result.auditType mustBe "ManageGoodsRecord"
      result.detail mustBe auditUpdateRecordDetailJson("SUCCEEDED", OK, Some(createOrUpdateRecordResponseData))
    }

    "return a ExtendedDataEvent with failure reason" in {
      val failureReason = Seq("error-1", "error-2")
      val result        = sut.updateRecord(
        updateRecordRequestData,
        timestamp.toString,
        "BAD_REQUEST",
        BAD_REQUEST,
        Some(failureReason)
      )

      result.auditSource mustBe "trader-goods-profiles-router"
      result.auditType mustBe "ManageGoodsRecord"
      result.detail mustBe auditDetailJsonWithFailureReason(
        failureReason,
        auditUpdateRecordDetailJson("BAD_REQUEST", BAD_REQUEST)
      )
    }
  }

  private def auditRemoveRecordDetailJson(status: String, statusCode: Int) =
    Json.obj(
      "journey"          -> "RemoveRecord",
      "clientId"         -> hc.headers(Seq("X-Client-ID")).head._2,
      "requestDateTime"  -> timestamp.toString,
      "responseDateTime" -> "2021-12-17T09:30:47Z",
      "request"          -> Json.obj(
        "eori"     -> eori,
        "recordId" -> recordId,
        "actorId"  -> actorId
      ),
      "outcome"          -> Json.obj(
        "status"     -> status,
        "statusCode" -> statusCode
      )
    )

  private def auditCreateRecordDetailJson(
    status: String,
    statusCode: Int,
    createOrUpdateRecordResponse: Option[CreateOrUpdateRecordResponse] = None
  ) =
    removeNulls(
      Json.obj(
        "journey"          -> "CreateRecord",
        "clientId"         -> hc.headers(Seq("X-Client-ID")).head._2,
        "requestDateTime"  -> timestamp.toString,
        "responseDateTime" -> "2021-12-17T09:30:47Z",
        "request"          -> createRecordRequestData,
        "outcome"          -> Json.obj(
          "status"     -> status,
          "statusCode" -> statusCode
        ),
        "response"         -> Some(createOrUpdateRecordResponse)
      )
    )

  private def auditUpdateRecordDetailJson(
    status: String,
    statusCode: Int,
    createOrUpdateRecordResponse: Option[CreateOrUpdateRecordResponse] = None
  ) =
    removeNulls(
      Json.obj(
        "journey"          -> "UpdateRecord",
        "clientId"         -> hc.headers(Seq("X-Client-ID")).head._2,
        "requestDateTime"  -> timestamp.toString,
        "responseDateTime" -> "2021-12-17T09:30:47Z",
        "request"          -> updateRecordRequestData,
        "outcome"          -> Json.obj(
          "status"     -> status,
          "statusCode" -> statusCode
        ),
        "response"         -> Some(createOrUpdateRecordResponse)
      )
    )

  private def auditDetailJsonWithFailureReason(failureReason: Seq[String], eventData: JsObject) =
    eventData
      .transform(
        __.json.update((__ \ "outcome" \ "failureReason").json.put(Json.toJson(failureReason)))
      )
      .get

  lazy val createOrUpdateRecordResponseData: CreateOrUpdateRecordResponse = Json
    .parse("""
             |{
             |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
             |  "eori": "GB123456789012",
             |  "actorId": "GB098765432112",
             |  "traderRef": "BAN001001",
             |  "comcode": "10410100",
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
    .as[CreateOrUpdateRecordResponse]

  lazy val createRecordRequestData: CreateRecordRequest = Json
    .parse("""
             |{
             |    "eori": "GB123456789012",
             |    "actorId": "GB098765432112",
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
    .as[CreateRecordRequest]

  lazy val updateRecordRequestData: UpdateRecordRequest =
    Json
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
      .as[UpdateRecordRequest]
}
