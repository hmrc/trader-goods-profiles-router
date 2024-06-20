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
import uk.gov.hmrc.tradergoodsprofilesrouter.factories.AuditEventFactory
import uk.gov.hmrc.tradergoodsprofilesrouter.models.ResponseModelSupport.removeNulls
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class AuditServiceSpec extends PlaySpec with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders = Seq(("X-Client-ID", "TSS")))

  private val auditConnector         = mock[AuditConnector]
  private val auditEventFactory      = mock[AuditEventFactory]
  private val dateTime               = Instant.parse("2021-12-17T09:30:47Z").toString
  private val eori                   = "GB123456789011"
  private val recordId               = "d677693e-9981-4ee3-8574-654981ebe606"
  private val actorId                = "GB123456789011"
  private val auditRemoveRecordEvent = extendedDataEvent(auditRemoveRecordDetailsJson("SUCCEEDED", NO_CONTENT))
  private val auditCreateRecordEvent = extendedDataEvent(
    auditCreateRecordDetailJson("SUCCEEDED", OK, Some(createRecordResponseData))
  )

  val sut = new AuditService(auditConnector, auditEventFactory)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(auditConnector, auditEventFactory)

    when(auditEventFactory.createRemoveRecord(any, any, any, any, any, any, any)(any))
      .thenReturn(auditRemoveRecordEvent)
    when(auditEventFactory.createRecord(any, any, any, any, any, any)(any))
      .thenReturn(auditCreateRecordEvent)
  }

  "auditRemoveRecord" should {
    "send an event for success response" in {

      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(sut.auditRemoveRecord(eori, recordId, actorId, dateTime, "SUCCEEDED", NO_CONTENT))

      result mustBe Done
      verify(auditEventFactory).createRemoveRecord(eori, recordId, actorId, dateTime, "SUCCEEDED", NO_CONTENT)
      verify(auditConnector).sendExtendedEvent(auditRemoveRecordEvent)
    }

    "send an event with reason failure" in {

      val auditEventWithFailure =
        extendedDataEvent(
          auditDetailJsonWithFailureReason(
            Seq("erro-1", "error-2"),
            auditRemoveRecordDetailsJson("BAD_REQUEST", BAD_REQUEST)
          )
        )
      when(auditEventFactory.createRemoveRecord(any, any, any, any, any, any, any)(any))
        .thenReturn(auditEventWithFailure)

      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(
        sut.auditRemoveRecord(
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
      verify(auditEventFactory).createRemoveRecord(
        eori,
        recordId,
        actorId,
        dateTime,
        "BAD_REQUEST",
        BAD_REQUEST,
        Some(Seq("erro-1", "error-2"))
      )
      verify(auditConnector).sendExtendedEvent(auditEventWithFailure)
    }
  }

  "auditCreateRecord" should {
    "send an event for success response" in {
      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(
        sut.auditCreateRecord(createRecordRequestData, dateTime, "SUCCEEDED", OK, None, Some(createRecordResponseData))
      )

      result mustBe Done
      verify(auditEventFactory)
        .createRecord(createRecordRequestData, dateTime, "SUCCEEDED", OK, None, Some(createRecordResponseData))
      verify(auditConnector).sendExtendedEvent(auditCreateRecordEvent)
    }

    "send an event with reason failure" in {

      val auditEventWithFailure =
        extendedDataEvent(
          auditDetailJsonWithFailureReason(
            Seq("erro-1", "error-2"),
            auditCreateRecordDetailJson("BAD_REQUEST", BAD_REQUEST)
          )
        )
      when(auditEventFactory.createRecord(any, any, any, any, any, any)(any))
        .thenReturn(auditEventWithFailure)

      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(
        sut.auditCreateRecord(
          createRecordRequestData,
          dateTime,
          "BAD_REQUEST",
          BAD_REQUEST,
          Some(Seq("erro-1", "error-2"))
        )
      )

      result mustBe Done
      verify(auditEventFactory)
        .createRecord(createRecordRequestData, dateTime, "BAD_REQUEST", BAD_REQUEST, Some(Seq("erro-1", "error-2")))
      verify(auditConnector).sendExtendedEvent(auditEventWithFailure)
    }
  }

  private def extendedDataEvent(auditDetails: JsValue) =
    ExtendedDataEvent(
      auditSource = "trader-goods-profiles-router",
      auditType = "ManageGoodsRecord",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )

  private def auditRemoveRecordDetailsJson(status: String, statusCode: Int) =
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

  private def auditCreateRecordDetailJson(
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

  private def auditDetailJsonWithFailureReason(failureReason: Seq[String], eventData: JsObject) =
    eventData
      .transform(
        __.json.update((__ \ "outcome" \ "failureReason").json.put(Json.toJson(failureReason)))
      )
      .get

  lazy val createRecordResponseData: CreateOrUpdateRecordResponse = Json
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
}
