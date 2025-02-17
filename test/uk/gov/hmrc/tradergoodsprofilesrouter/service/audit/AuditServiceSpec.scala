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

package uk.gov.hmrc.tradergoodsprofilesrouter.service.audit

import org.apache.pekko.Done
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.{JsObject, JsValue, Json, __}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.tradergoodsprofilesrouter.models.CreateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.models.ResponseModelSupport.removeNulls
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.AuditCreateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.request.AuditUpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.response.{AuditCreateRecordResponse, AuditUpdateRecordResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.payloads.UpdateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec extends PlaySpec with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders = Seq(("X-Client-ID", "TSS")))

  private val auditConnector  = mock[AuditConnector]
  private val dataTimeService = mock[DateTimeService]
  private val timestamp       = Instant.parse("2021-12-17T09:30:47.456Z")
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
      val extendedDataEventCaptor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(auditConnector).sendExtendedEvent(extendedDataEventCaptor.capture)(any, any)

      val actualEvent = extendedDataEventCaptor.getValue
      actualEvent.auditSource mustBe sut.auditSource

    }

    "send an event with reason failure" in {

      when(auditConnector.sendExtendedEvent(any)(any, any))
        .thenReturn(Future.successful(AuditResult.Success)) // Mock audit response

      val failureReason = Some(Seq("Some failure reason")) // Simulating failure reason
      val result        = await(
        sut.emitAuditRemoveRecord(eori, recordId, actorId, dateTime, "FAILED", INTERNAL_SERVER_ERROR, failureReason)
      )

      result mustBe Done

      val extendedDataEventCaptor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(auditConnector).sendExtendedEvent(extendedDataEventCaptor.capture())(any, any)

      val actualEvent = extendedDataEventCaptor.getValue
      actualEvent.auditSource mustBe sut.auditSource

      val detailJson = Json.stringify(actualEvent.detail)
      detailJson must include("FAILED")
      detailJson must include("Some failure reason")
    }
  }

  "emitAuditCreateRecord" should {
    "send an event for success response" in {
      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(
        sut.emitAuditCreateRecord(
          createRecordRequestDataPayload,
          dateTime,
          "SUCCEEDED",
          OK,
          None,
          Some(createOrUpdateRecordResponseData)
        )
      )

      result mustBe Done
      val extendedDataEventCaptor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(auditConnector).sendExtendedEvent(extendedDataEventCaptor.capture)(any, any)
      val actualEvent             = extendedDataEventCaptor.getValue
      actualEvent.detail mustBe emitAuditCreateRecordDetailJson(
        "SUCCEEDED",
        OK,
        auditCreateRecordRequestData,
        Some(auditCreateRecordResponseData)
      )
      actualEvent.auditType mustBe auditType
      actualEvent.auditSource mustBe auditSource
    }

    "send an event for success response without assessments" in {
      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(
        sut.emitAuditCreateRecord(
          createRecordRequestDataPayloadWithoutAssessments,
          dateTime,
          "SUCCEEDED",
          OK,
          None,
          Some(createOrUpdateRecordResponseDataWithoutAssessments)
        )
      )

      result mustBe Done
      val extendedDataEventCaptor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(auditConnector).sendExtendedEvent(extendedDataEventCaptor.capture)(any, any)
      val actualEvent             = extendedDataEventCaptor.getValue
      actualEvent.detail mustBe emitAuditCreateRecordDetailJson(
        "SUCCEEDED",
        OK,
        auditCreateRecordRequestDataWithoutAssessments,
        Some(auditCreateRecordResponseData)
      )
      actualEvent.auditType mustBe auditType
      actualEvent.auditSource mustBe auditSource
    }

    "send an event with reason failure" in {

      val auditEventWithFailure =
        extendedDataEvent(
          auditDetailJsonWithFailureReason(
            Seq("erro-1", "error-2"),
            emitAuditCreateRecordDetailJson("BAD_REQUEST", BAD_REQUEST, auditCreateRecordRequestData)
          )
        )

      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(
        sut.emitAuditCreateRecord(
          createRecordRequestDataPayload,
          dateTime,
          "BAD_REQUEST",
          BAD_REQUEST,
          Some(Seq("erro-1", "error-2"))
        )
      )

      result mustBe Done
      val extendedDataEventCaptor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(auditConnector).sendExtendedEvent(extendedDataEventCaptor.capture)(any, any)
      val actualEvent             = extendedDataEventCaptor.getValue
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
          updateRecordRequestDataPayload,
          dateTime,
          "SUCCEEDED",
          OK,
          None,
          Some(createOrUpdateRecordResponseData)
        )
      )

      result mustBe Done
      val extendedDataEventCaptor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(auditConnector).sendExtendedEvent(extendedDataEventCaptor.capture)(any, any)
      val actualEvent             = extendedDataEventCaptor.getValue
      actualEvent.detail mustBe emitAuditUpdateRecordDetailJson("SUCCEEDED", OK, Some(auditUpdateRecordResponseData))
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
          updateRecordRequestDataPayload,
          dateTime,
          "BAD_REQUEST",
          BAD_REQUEST,
          Some(Seq("erro-1", "error-2"))
        )
      )

      result mustBe Done
      val extendedDataEventCaptor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(auditConnector).sendExtendedEvent(extendedDataEventCaptor.capture)(any, any)
      val actualEvent             = extendedDataEventCaptor.getValue
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
    auditCreateRecordRequest: AuditCreateRecordRequest,
    auditCreateRecordResponse: Option[AuditCreateRecordResponse] = None
  ) =
    removeNulls(
      Json.obj(
        "journey"          -> "CreateRecord",
        "clientId"         -> hc.headers(Seq("X-Client-ID")).head._2,
        "requestDateTime"  -> dateTime,
        "responseDateTime" -> dateTime,
        "request"          -> auditCreateRecordRequest,
        "outcome"          -> Json.obj(
          "status"     -> status,
          "statusCode" -> statusCode
        ),
        "response"         -> Some(auditCreateRecordResponse)
      )
    )

  private def emitAuditUpdateRecordDetailJson(
    status: String,
    statusCode: Int,
    auditUpdateRecordResponse: Option[AuditUpdateRecordResponse] = None
  ) =
    removeNulls(
      Json.obj(
        "journey"          -> "UpdateRecord",
        "clientId"         -> hc.headers(Seq("X-Client-ID")).head._2,
        "requestDateTime"  -> dateTime,
        "responseDateTime" -> dateTime,
        "request"          -> auditUpdateRecordRequestData,
        "outcome"          -> Json.obj(
          "status"     -> status,
          "statusCode" -> statusCode
        ),
        "response"         -> Some(auditUpdateRecordResponse)
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
             |  "adviceStatus": "Not Requested",
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

  lazy val createOrUpdateRecordResponseDataWithoutAssessments: CreateOrUpdateRecordResponse = Json
    .parse("""
             |{
             |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
             |  "eori": "GB123456789012",
             |  "actorId": "GB098765432112",
             |  "traderRef": "BAN001001",
             |  "comcode": "10410100",
             |  "adviceStatus": "Not Requested",
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
             |  "updatedDateTime": "2024-11-18T23:20:19Z"
             |}
             |""".stripMargin)
    .as[CreateOrUpdateRecordResponse]

  lazy val auditCreateRecordResponseData: AuditCreateRecordResponse = Json
    .parse("""
             |{
             |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
             |  "adviceStatus": "Not Requested",
             |  "recordVersion": 1,
             |  "recordActive": true,
             |  "recordToReview": false,
             |  "reviewReason": "Commodity code change",
             |  "declarableStatus": "SPIMM",
             |  "UKIMSNumber": "XIUKIM47699357400020231115081800",
             |  "NIRMSNumber": "RMS-GB-123456",
             |  "NIPHLNumber": "6 S12345"
             |}
             |""".stripMargin)
    .as[AuditCreateRecordResponse]

  lazy val auditUpdateRecordResponseData: AuditUpdateRecordResponse = Json
    .parse("""
             |{
             |  "adviceStatus": "Not Requested",
             |  "recordVersion": 1,
             |  "recordActive": true,
             |  "recordToReview": false,
             |  "reviewReason": "Commodity code change",
             |  "declarableStatus": "SPIMM",
             |  "UKIMSNumber": "XIUKIM47699357400020231115081800",
             |  "NIRMSNumber": "RMS-GB-123456",
             |  "NIPHLNumber": "6 S12345"
             |}
             |""".stripMargin)
    .as[AuditUpdateRecordResponse]

  lazy val createRecordRequestDataPayload: CreateRecordPayload = Json
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
    .as[CreateRecordPayload]

  lazy val createRecordRequestDataPayloadWithoutAssessments: CreateRecordPayload = Json
    .parse("""
             |{
             |    "eori": "GB123456789012",
             |    "actorId": "GB098765432112",
             |    "traderRef": "BAN001001",
             |    "comcode": "10410100",
             |    "goodsDescription": "Organic bananas",
             |    "countryOfOrigin": "EC",
             |    "category": 1,
             |    "supplementaryUnit": 500,
             |    "measurementUnit": "Square metre (m2)",
             |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
             |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
             |}
             |""".stripMargin)
    .as[CreateRecordPayload]

  lazy val auditCreateRecordRequestData: AuditCreateRecordRequest = Json
    .parse("""
             |{
             |    "eori": "GB123456789012",
             |    "actorId": "GB098765432112",
             |    "goodsDescription": "Organic bananas",
             |    "traderReference": "BAN001001",
             |    "category": 1,
             |    "commodityCode": "10410100",
             |    "countryOfOrigin": "EC",
             |    "commodityCodeEffectiveFrom": "2024-11-18T23:20:19Z",
             |    "commodityCodeEffectiveTo": "2024-11-18T23:20:19Z",
             |    "supplementaryUnit": 500,
             |    "measurementUnit": "Square metre (m2)",
             |    "categoryAssessments": 1
             |}
             |""".stripMargin)
    .as[AuditCreateRecordRequest]

  lazy val auditCreateRecordRequestDataWithoutAssessments: AuditCreateRecordRequest = Json
    .parse("""
             |{
             |    "eori": "GB123456789012",
             |    "actorId": "GB098765432112",
             |    "goodsDescription": "Organic bananas",
             |    "traderReference": "BAN001001",
             |    "category": 1,
             |    "commodityCode": "10410100",
             |    "countryOfOrigin": "EC",
             |    "commodityCodeEffectiveFrom": "2024-11-18T23:20:19Z",
             |    "commodityCodeEffectiveTo": "2024-11-18T23:20:19Z",
             |    "supplementaryUnit": 500,
             |    "measurementUnit": "Square metre (m2)"
             |}
             |""".stripMargin)
    .as[AuditCreateRecordRequest]

  lazy val updateRecordRequestDataPayload: UpdateRecordPayload =
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
      .as[UpdateRecordPayload]

  lazy val auditUpdateRecordRequestData: AuditUpdateRecordRequest = Json
    .parse("""
             |{
             |    "eori": "GB123456789001",
             |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
             |    "actorId": "GB098765432112",
             |    "goodsDescription": "Organic bananas",
             |    "traderReference": "BAN001001",
             |    "category": 1,
             |    "commodityCode": "10410100",
             |    "countryOfOrigin": "EC",
             |    "commodityCodeEffectiveFrom": "2024-11-18T23:20:19Z",
             |    "commodityCodeEffectiveTo": "2024-11-18T23:20:19Z",
             |    "supplementaryUnit": 500,
             |    "measurementUnit": "Square metre (m2)",
             |    "categoryAssessments": 1
             |}
             |""".stripMargin)
    .as[AuditUpdateRecordRequest]
}
