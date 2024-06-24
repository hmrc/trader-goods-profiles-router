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

package uk.gov.hmrc.tradergoodsprofilesrouter.service

import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK}
import play.api.libs.json.{JsObject, JsValue, Json, __}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.tradergoodsprofilesrouter.models.ResponseModelSupport.removeNulls
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.{CreateRecordRequest, UpdateRecordRequest}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec extends PlaySpec with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders = Seq(("X-Client-ID", "TSS")))

  private val auditConnector  = mock[AuditConnector]
  private val dataTimeService = mock[DateTimeService]
  private val timestamp       = Instant.parse("2021-12-17T09:30:47Z")
  private val dateTime        = timestamp.toString
  private val eori            = "GB123456789011"
  private val recordId        = "d677693e-9981-4ee3-8574-654981ebe606"
  private val actorId         = "GB123456789011"
  private val auditType       = "ManageGoodsRecord"
  private val auditSource     = "trader-goods-profiles-router"

  val sut = new AuditService(auditConnector, dataTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(auditConnector, dataTimeService)
    when(dataTimeService.timestamp).thenReturn(timestamp)
  }

  "emitAuditRemoveRecord" should {
    "send an event for success response" in {

      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(sut.emitAuditRemoveRecord(eori, recordId, actorId, dateTime, "SUCCEEDED", NO_CONTENT))

      result mustBe Done
      val extendedDateEventCaptor = ArgCaptor[ExtendedDataEvent]
      verify(auditConnector).sendExtendedEvent(extendedDateEventCaptor.capture)(any, any)
      val actualEvent             = extendedDateEventCaptor.value
      actualEvent.detail mustBe emitAuditRemoveRecordDetailsJson("SUCCEEDED", NO_CONTENT)
      actualEvent.auditType mustBe auditType
      actualEvent.auditSource mustBe auditSource
    }

    "send an event with reason failure" in {

      val auditEventWithFailure =
        extendedDataEvent(
          auditDetailJsonWithFailureReason(
            Seq("erro-1", "error-2"),
            emitAuditRemoveRecordDetailsJson("BAD_REQUEST", BAD_REQUEST)
          )
        )

      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(
        sut.emitAuditRemoveRecord(
          eori,
          recordId,
          actorId,
          dateTime,
          "BAD_REQUEST",
          BAD_REQUEST,
          Some(Seq("erro-1", "error-2"))
        )
      )

      result mustBe Done
      val extendedDateEventCaptor = ArgCaptor[ExtendedDataEvent]
      verify(auditConnector).sendExtendedEvent(extendedDateEventCaptor.capture)(any, any)
      val actualEvent             = extendedDateEventCaptor.value
      actualEvent.detail mustBe auditEventWithFailure.detail
      actualEvent.auditType mustBe auditType
      actualEvent.auditSource mustBe auditSource
    }
  }

  "emitAuditCreateRecord" should {
    "send an event for success response" in {
      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(
        sut.emitAuditCreateRecord(
          createRecordRequestData,
          dateTime,
          "SUCCEEDED",
          OK,
          None,
          Some(createOrUpdateRecordResponseData)
        )
      )

      result mustBe Done
      val extendedDateEventCaptor = ArgCaptor[ExtendedDataEvent]
      verify(auditConnector).sendExtendedEvent(extendedDateEventCaptor.capture)(any, any)
      val actualEvent             = extendedDateEventCaptor.value
      actualEvent.detail mustBe emitAuditCreateRecordDetailJson("SUCCEEDED", OK, Some(createOrUpdateRecordResponseData))
      actualEvent.auditType mustBe auditType
      actualEvent.auditSource mustBe auditSource
    }

    "send an event with reason failure" in {

      val auditEventWithFailure =
        extendedDataEvent(
          auditDetailJsonWithFailureReason(
            Seq("erro-1", "error-2"),
            emitAuditCreateRecordDetailJson("BAD_REQUEST", BAD_REQUEST)
          )
        )

      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(
        sut.emitAuditCreateRecord(
          createRecordRequestData,
          dateTime,
          "BAD_REQUEST",
          BAD_REQUEST,
          Some(Seq("erro-1", "error-2"))
        )
      )

      result mustBe Done
      val extendedDateEventCaptor = ArgCaptor[ExtendedDataEvent]
      verify(auditConnector).sendExtendedEvent(extendedDateEventCaptor.capture)(any, any)
      val actualEvent             = extendedDateEventCaptor.value
      actualEvent.detail mustBe auditEventWithFailure.detail
      actualEvent.auditType mustBe auditType
      actualEvent.auditSource mustBe auditSource
    }
  }

  "emitAuditUpdateRecord" should {
    "send an event for success response" in {
      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(
        sut.emitAuditUpdateRecord(
          updateRecordRequestData,
          dateTime,
          "SUCCEEDED",
          OK,
          None,
          Some(createOrUpdateRecordResponseData)
        )
      )

      result mustBe Done
      val extendedDateEventCaptor = ArgCaptor[ExtendedDataEvent]
      verify(auditConnector).sendExtendedEvent(extendedDateEventCaptor.capture)(any, any)
      val actualEvent             = extendedDateEventCaptor.value
      actualEvent.detail mustBe emitAuditUpdateRecordDetailJson("SUCCEEDED", OK, Some(createOrUpdateRecordResponseData))
      actualEvent.auditType mustBe auditType
      actualEvent.auditSource mustBe auditSource
    }

    "send an event with reason failure" in {

      val auditEventWithFailure =
        extendedDataEvent(
          auditDetailJsonWithFailureReason(
            Seq("erro-1", "error-2"),
            emitAuditUpdateRecordDetailJson("BAD_REQUEST", BAD_REQUEST)
          )
        )

      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(
        sut.emitAuditUpdateRecord(
          updateRecordRequestData,
          dateTime,
          "BAD_REQUEST",
          BAD_REQUEST,
          Some(Seq("erro-1", "error-2"))
        )
      )

      result mustBe Done
      val extendedDateEventCaptor = ArgCaptor[ExtendedDataEvent]
      verify(auditConnector).sendExtendedEvent(extendedDateEventCaptor.capture)(any, any)
      val actualEvent             = extendedDateEventCaptor.value
      actualEvent.detail mustBe auditEventWithFailure.detail
      actualEvent.auditType mustBe auditType
      actualEvent.auditSource mustBe auditSource
    }
  }

  private def extendedDataEvent(auditDetails: JsValue) =
    ExtendedDataEvent(
      auditSource = "trader-goods-profiles-router",
      auditType = "ManageGoodsRecord",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )

  private def emitAuditRemoveRecordDetailsJson(status: String, statusCode: Int) =
    Json.obj(
      "journey"          -> "RemoveRecord",
      "clientId"         -> hc.headers(Seq("X-Client-ID")).head._2,
      "requestDateTime"  -> dateTime,
      "responseDateTime" -> dateTime,
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

  private def emitAuditCreateRecordDetailJson(
    status: String,
    statusCode: Int,
    createOrUpdateRecordResponse: Option[CreateOrUpdateRecordResponse] = None
  ) =
    removeNulls(
      Json.obj(
        "journey"          -> "CreateRecord",
        "clientId"         -> hc.headers(Seq("X-Client-ID")).head._2,
        "requestDateTime"  -> dateTime,
        "responseDateTime" -> dateTime,
        "request"          -> createRecordRequestData,
        "outcome"          -> Json.obj(
          "status"     -> status,
          "statusCode" -> statusCode
        ),
        "response"         -> Some(createOrUpdateRecordResponse)
      )
    )

  private def emitAuditUpdateRecordDetailJson(
    status: String,
    statusCode: Int,
    createOrUpdateRecordResponse: Option[CreateOrUpdateRecordResponse] = None
  ) =
    removeNulls(
      Json.obj(
        "journey"          -> "UpdateRecord",
        "clientId"         -> hc.headers(Seq("X-Client-ID")).head._2,
        "requestDateTime"  -> dateTime,
        "responseDateTime" -> dateTime,
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
