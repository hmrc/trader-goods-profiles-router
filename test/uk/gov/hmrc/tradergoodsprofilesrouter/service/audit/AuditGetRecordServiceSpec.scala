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
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.request.AuditGetRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.AccreditationStatus.{NotRequested, Requested, Withdrawn}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis._
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class AuditGetRecordServiceSpec extends PlaySpec with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders = Seq(("X-Client-ID", "TSS")))

  private val auditConnector  = mock[AuditConnector]
  private val dataTimeService = mock[DateTimeService]
  private val timestamp       = Instant.parse("2021-12-17T09:30:47.456Z")
  private val dateTime        = timestamp.toString
  private val eori            = "GB123456789011"
  private val recordId        = "d677693e-9981-4ee3-8574-654981ebe606"
  private val sut             = new AuditGetRecordService(auditConnector, dataTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(auditConnector, dataTimeService)
    when(dataTimeService.timestamp).thenReturn(timestamp)
    when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))
  }

  "emitAuditForGetRecords" should {
    "send an event for a successful response" when {
      "all three query parameter are present" in {
        val request = AuditGetRecordRequest(eori, Some(dateTime), Some(1), Some(10), Some(recordId))

        val result = await(
          sut.emitAuditGetRecord(
            request,
            dateTime,
            "SUCCEEDED",
            200,
            createEisSingleRecordsResponse
          )
        )

        result mustBe Done
        val evt: ExtendedDataEvent = verifyAndReturnEvent
        evt.detail mustBe expectedDetailsWithQueryParameters

        withClue("return the right auditSource and auditType") {
          evt.auditSource mustBe "trader-goods-profiles-router"
          evt.auditType mustBe "GetGoodsRecord"
        }
      }

      "query parameter are missing" in {
        val request = AuditGetRecordRequest(eori = eori)

        val result = await(
          sut.emitAuditGetRecord(
            request,
            dateTime,
            "SUCCEEDED",
            200,
            createEisSingleRecordsResponse
          )
        )

        result mustBe Done
        verifyAndReturnEvent.detail mustBe expectedDetailsWithoutQueryParameters
      }

      "multiple records" in {
        val request = AuditGetRecordRequest(eori, Some(dateTime), Some(1), Some(10), Some(recordId))

        val result = await(
          sut.emitAuditGetRecord(
            request,
            dateTime,
            "SUCCEEDED",
            200,
            createEisMultipleRecordsResponse
          )
        )

        result mustBe Done
        verifyAndReturnEvent.detail mustBe expectedDetailsForMultipleRecords
      }

      "no record found" in {
        val request = AuditGetRecordRequest(eori, Some(dateTime), Some(1), Some(10), Some(recordId))

        val result = await(
          sut.emitAuditGetRecord(
            request,
            dateTime,
            "SUCCEEDED",
            200,
            GetEisRecordsResponse(Seq.empty, Pagination(0, 0, 0, None, None))
          )
        )

        result mustBe Done
        verifyAndReturnEvent.detail mustBe expectedDetailsForNoRecordFound
      }
    }

    "send an event for a failure response" when {
      val errorMessages = Seq("Error message 1", "Error message 2", "Error message 3")

      "all three query parameter are present" in {
        val request = AuditGetRecordRequest(eori, Some(dateTime), Some(1), Some(10), Some(recordId))

        val result = await(
          sut.emitAuditGetRecordFailure(
            request,
            dateTime,
            "FORBIDDEN",
            403,
            errorMessages
          )
        )

        result mustBe Done
        val evt: ExtendedDataEvent = verifyAndReturnEvent
        evt.detail mustBe expectedFailureEvent(errorMessages: _*)

        withClue("return the right auditSource and auditType") {
          evt.auditSource mustBe "trader-goods-profiles-router"
          evt.auditType mustBe "GetGoodsRecord"
        }
      }

      "query parameter are missing" in {
        val request = AuditGetRecordRequest(eori = eori)

        val result = await(
          sut.emitAuditGetRecordFailure(
            request,
            dateTime,
            "FORBIDDEN",
            403,
            errorMessages
          )
        )

        result mustBe Done
        verifyAndReturnEvent.detail mustBe expectedFailureEventWithoutRequestParams(errorMessages: _*)
      }
    }
  }

  private def verifyAndReturnEvent = {
    val captor = ArgCaptor[ExtendedDataEvent]
    verify(auditConnector).sendExtendedEvent(captor.capture)(any, any)

    captor.value
  }

  private def createEisSingleRecordsResponse =
    GetEisRecordsResponse(
      Seq(createEISGoodsItemRecord("IMMI Ready", true, Some("mismatch"), false, true, NotRequested, Some(3))),
      Pagination(1, 1, 1, None, None)
    )

  private def createEisMultipleRecordsResponse: GetEisRecordsResponse = {

    val declarable = Seq(
      "IMMI Ready",
      "Not Ready for IMMI",
      "Not Ready For Use",
      "IMMI Ready",
      "Not Ready for IMMI"
    )

    val review              = Seq(true, false, true, false, true)
    val reviewReason        = Seq("Commodity code changed", "Expired", "Expired", "Expired", "Commodity code changed")
    val locked              = Seq(true, false, true, false, true)
    val active              = Seq(true, false, true, false, true)
    val accreditationStatus = Seq(NotRequested, Withdrawn, NotRequested, Requested, Withdrawn)
    val categories          = Seq(1, 2, 3, 2, 1)

    val records = for {
      i     <- 0 until 5
      record = createEISGoodsItemRecord(
                 declarable(i),
                 review(i),
                 Some(reviewReason(i)),
                 locked(i),
                 active(i),
                 accreditationStatus(i),
                 Some(categories(i))
               )
    } yield record

    GetEisRecordsResponse(records, Pagination(5, 0, 1, None, None))
  }

  private def createEISGoodsItemRecord(
    declarable: String,
    review: Boolean,
    reviewReason: Option[String],
    locked: Boolean,
    active: Boolean,
    accreditationStatus: AccreditationStatus,
    category: Option[Int]
  )                                                 =
    EisGoodsItemRecords(
      eori = eori,
      actorId = "GB1234567890",
      recordId = recordId,
      traderRef = "BAN001001",
      comcode = "10410100",
      accreditationStatus = accreditationStatus,
      goodsDescription = "Organic bananas",
      countryOfOrigin = "EC",
      category = category,
      assessments = None,
      supplementaryUnit = Some(BigDecimal(500)),
      measurementUnit = Some("square meters(m^2)"),
      comcodeEffectiveFromDate = Instant.now,
      comcodeEffectiveToDate = None,
      version = 1,
      active = active,
      toReview = review,
      reviewReason = reviewReason,
      declarable = declarable,
      ukimsNumber = "XIUKIM47699357400020231115081800",
      nirmsNumber = Some("RMS-GB-123456"),
      niphlNumber = Some("--1234"),
      locked = locked,
      createdDateTime = Instant.now,
      updatedDateTime = Instant.now
    )
  private def expectedDetailsWithoutQueryParameters =
    Json.obj(
      "clientId"         -> "TSS",
      "requestDateTime"  -> timestamp,
      "responseDateTime" -> timestamp,
      "outcome"          -> Json.obj(
        "status"     -> "SUCCEEDED",
        "statusCode" -> 200
      ),
      "request"          -> Json.obj(
        "eori" -> eori
      ),
      "response"         -> Json.obj(
        "totalRecords"        -> 1,
        "payloadRecords"      -> 1,
        "currentPage"         -> 1,
        "totalPages"          -> 1,
        "IMMIReadyCount"      -> 1,
        "notIMMIReadyCount"   -> 0,
        "notReadyForUseCount" -> 0,
        "forReviewCount"      -> 1,
        "reviewReasons"       -> Json.obj("mismatch" -> 1),
        "lockedCount"         -> 0,
        "activeCount"         -> 1,
        "adviceStatuses"      -> Json.obj("Not Requested" -> 1),
        "categories"          -> Json.obj("3" -> 1),
        "UKIMSNumber"         -> "XIUKIM47699357400020231115081800",
        "NIRMSNumber"         -> "RMS-GB-123456",
        "NIPHLNumber"         -> "--1234"
      )
    )

  private def expectedDetailsForMultipleRecords =
    Json.obj(
      "clientId"         -> "TSS",
      "requestDateTime"  -> timestamp,
      "responseDateTime" -> timestamp,
      "outcome"          -> Json.obj(
        "status"     -> "SUCCEEDED",
        "statusCode" -> 200
      ),
      "request"          -> Json.obj(
        "eori"            -> eori,
        "lastUpdatedDate" -> dateTime,
        "page"            -> 1,
        "size"            -> 10,
        "recordId"        -> recordId
      ),
      "response"         -> Json.obj(
        "totalRecords"        -> 5,
        "payloadRecords"      -> 5,
        "currentPage"         -> 0,
        "totalPages"          -> 1,
        "IMMIReadyCount"      -> 2,
        "notIMMIReadyCount"   -> 2,
        "notReadyForUseCount" -> 1,
        "forReviewCount"      -> 3,
        "reviewReasons"       -> Json.obj("Commodity code changed" -> 2, "Expired" -> 3),
        "lockedCount"         -> 3,
        "activeCount"         -> 3,
        "adviceStatuses"      -> Json.obj("Not Requested" -> 2, "Withdrawn" -> 2, "Requested" -> 1),
        "categories"          -> Json.obj("1" -> 2, "2" -> 2, "3" -> 1),
        "UKIMSNumber"         -> "XIUKIM47699357400020231115081800",
        "NIRMSNumber"         -> "RMS-GB-123456",
        "NIPHLNumber"         -> "--1234"
      )
    )

  private def expectedDetailsWithQueryParameters =
    Json.obj(
      "clientId"         -> "TSS",
      "requestDateTime"  -> timestamp,
      "responseDateTime" -> timestamp,
      "outcome"          -> Json.obj(
        "status"     -> "SUCCEEDED",
        "statusCode" -> 200
      ),
      "request"          -> Json.obj(
        "eori"            -> eori,
        "lastUpdatedDate" -> dateTime,
        "page"            -> 1,
        "size"            -> 10,
        "recordId"        -> recordId
      ),
      "response"         -> Json.obj(
        "totalRecords"        -> 1,
        "payloadRecords"      -> 1,
        "currentPage"         -> 1,
        "totalPages"          -> 1,
        "IMMIReadyCount"      -> 1,
        "notIMMIReadyCount"   -> 0,
        "notReadyForUseCount" -> 0,
        "forReviewCount"      -> 1,
        "reviewReasons"       -> Json.obj("mismatch" -> 1),
        "lockedCount"         -> 0,
        "activeCount"         -> 1,
        "adviceStatuses"      -> Json.obj("Not Requested" -> 1),
        "categories"          -> Json.obj("3" -> 1),
        "UKIMSNumber"         -> "XIUKIM47699357400020231115081800",
        "NIRMSNumber"         -> "RMS-GB-123456",
        "NIPHLNumber"         -> "--1234"
      )
    )

  private def expectedDetailsForNoRecordFound =
    Json.obj(
      "clientId"         -> "TSS",
      "requestDateTime"  -> timestamp,
      "responseDateTime" -> timestamp,
      "outcome"          -> Json.obj(
        "status"     -> "SUCCEEDED",
        "statusCode" -> 200
      ),
      "request"          -> Json.obj(
        "eori"            -> eori,
        "lastUpdatedDate" -> dateTime,
        "page"            -> 1,
        "size"            -> 10,
        "recordId"        -> recordId
      ),
      "response"         -> Json.obj(
        "totalRecords"   -> 0,
        "payloadRecords" -> 0,
        "currentPage"    -> 0,
        "totalPages"     -> 0
      )
    )

  private def expectedFailureEvent(errors: String*) =
    Json.obj(
      "clientId"         -> "TSS",
      "requestDateTime"  -> timestamp,
      "responseDateTime" -> timestamp,
      "outcome"          -> Json.obj(
        "status"        -> "FORBIDDEN",
        "statusCode"    -> 403,
        "failureReason" -> errors
      ),
      "request"          -> Json.obj(
        "eori"            -> eori,
        "lastUpdatedDate" -> dateTime,
        "page"            -> 1,
        "size"            -> 10,
        "recordId"        -> recordId
      )
    )

  private def expectedFailureEventWithoutRequestParams(errors: String*) =
    Json.obj(
      "clientId"         -> "TSS",
      "requestDateTime"  -> timestamp,
      "responseDateTime" -> timestamp,
      "outcome"          -> Json.obj(
        "status"        -> "FORBIDDEN",
        "statusCode"    -> 403,
        "failureReason" -> errors
      ),
      "request"          -> Json.obj(
        "eori" -> eori
      )
    )
}
