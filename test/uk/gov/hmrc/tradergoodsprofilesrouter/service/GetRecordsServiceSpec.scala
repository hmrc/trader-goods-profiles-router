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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{EisHttpErrorResponse, GetRecordsConnector}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.request.AuditGetRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.GetRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.audit.AuditGetRecordService
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class GetRecordsServiceSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with GetRecordsDataSupport
    with EitherValues
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val eoriNumber            = "GB123456789011"
  private val recordId              = "recordId"
  private val correlationId         = "1234-5678-9012"
  private val getRecordsConnector   = mock[GetRecordsConnector]
  private val uuidService           = mock[UuidService]
  private val auditGetRecordService = mock[AuditGetRecordService]
  private val dateTimeService       = mock[DateTimeService]
  private val timestamp             = Instant.parse("2024-05-06T14:14:14.233Z")
  private val urlPath               = s"http://localhost:1234/tgp/getrecords/v1/"

  val sut = new GetRecordsService(getRecordsConnector, uuidService, dateTimeService, auditGetRecordService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(getRecordsConnector, uuidService, auditGetRecordService)
    when(uuidService.uuid).thenReturn(correlationId)
    when(dateTimeService.timestamp).thenReturn(timestamp)
  }

  "fetchRecord" should {
    "return a record item" in {
      val eisResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]
      when(getRecordsConnector.fetchRecord(any, any, any, any)(any))
        .thenReturn(Future.successful(Right(eisResponse)))

      val result = await(sut.fetchRecord(eoriNumber, recordId, urlPath))

      result.value shouldBe getRecordsResponseData.as[GetRecordsResponse].goodsItemRecords.head

      withClue("should call the connector with the right parameters") {
        verify(getRecordsConnector).fetchRecord(
          eqTo(eoriNumber),
          eqTo(recordId),
          eqTo(correlationId),
          eqTo(urlPath)
        )(any)
      }

      withClue("should send an audit event") {
        verify(auditGetRecordService).emitAuditGetRecordSucceeded(
          eqTo(AuditGetRecordRequest(eori = eoriNumber, recordId = Some(recordId))),
          eqTo("2024-05-06T14:14:14.233Z"),
          eqTo(GetRecordsResponse(eisResponse))
        )(any)
      }
    }

    "return an error" when {
      "EIS return an error" in {
        val badRequestErrorResponse = createEisErrorResponse

        when(getRecordsConnector.fetchRecord(any, any, any, any)(any))
          .thenReturn(Future.successful(Left(badRequestErrorResponse)))

        val result = await(sut.fetchRecord(eoriNumber, recordId, urlPath))

        result.left.value shouldBe badRequestErrorResponse

        withClue("should send an audit event") {
          verify(auditGetRecordService).emitAuditGetRecordFailure(
            eqTo(AuditGetRecordRequest(eori = eoriNumber, recordId = Some(recordId))),
            eqTo("2024-05-06T14:14:14.233Z"),
            eqTo("BAD_REQUEST"),
            eqTo(BAD_REQUEST),
            eqTo(badRequestErrorResponse.errorResponse.errors.map(_.map(_.message)))
          )(any)
        }
      }

      "error when an exception is thrown" in {
        when(getRecordsConnector.fetchRecord(any, any, any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = await(sut.fetchRecord(eoriNumber, recordId, urlPath))

        result.left.value shouldBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error")
        )

        withClue("should send an audit event") {
          verify(auditGetRecordService).emitAuditGetRecordFailure(
            eqTo(AuditGetRecordRequest(eori = eoriNumber, recordId = Some(recordId))),
            eqTo("2024-05-06T14:14:14.233Z"),
            eqTo("UNEXPECTED_ERROR"),
            eqTo(INTERNAL_SERVER_ERROR),
            eqTo(None)
          )(any)
        }
      }

    }

  }

  "fetchRecords" should {

    val lastUpdateDate = Instant.parse("2024-04-18T23:20:19Z")

    "return a records" in {
      val eisResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]
      when(getRecordsConnector.fetchRecords(any, any, any, any, any)(any))
        .thenReturn(Future.successful(Right(eisResponse)))

      val result = await(sut.fetchRecords(eoriNumber, 1, Some(1), Some(lastUpdateDate)))

      result.value shouldBe getRecordsResponseData.as[GetRecordsResponse]

      withClue("should call the getRecordsConnector with the right parameters") {
        verify(getRecordsConnector)
          .fetchRecords(
            eqTo(eoriNumber),
            eqTo(correlationId),
            eqTo(1),
            eqTo(Some(1)),
            eqTo(Some(lastUpdateDate))
          )(any)
      }

      withClue("should send an audit event") {
        verify(auditGetRecordService).emitAuditGetRecordSucceeded(
          eqTo(AuditGetRecordRequest(eoriNumber, Some("2024-04-18T23:20:19.000Z"), Some(1), Some(1))),
          eqTo("2024-05-06T14:14:14.233Z"),
          eqTo(GetRecordsResponse(eisResponse))
        )(any)
      }
    }

    "return an error" when {

      "EIS return an error" in {
        val badRequestErrorResponse = createEisErrorResponse

        when(getRecordsConnector.fetchRecords(any, any, any, any, any)(any))
          .thenReturn(Future.successful(Left(badRequestErrorResponse)))

        val result = await(sut.fetchRecords(eoriNumber, 1, Some(1), Some(lastUpdateDate)))

        result.left.value shouldBe badRequestErrorResponse

        withClue("should send an audit event") {
          verify(auditGetRecordService).emitAuditGetRecordFailure(
            eqTo(AuditGetRecordRequest(eoriNumber, Some("2024-04-18T23:20:19.000Z"), Some(1), Some(1))),
            eqTo("2024-05-06T14:14:14.233Z"),
            eqTo(badRequestErrorResponse.errorResponse.code),
            eqTo(badRequestErrorResponse.httpStatus),
            eqTo(badRequestErrorResponse.errorResponse.errors.map(_.map(_.message)))
          )(any)
        }
      }

      "error when an exception is thrown" in {
        when(getRecordsConnector.fetchRecords(any, any, any, any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = await(sut.fetchRecords(eoriNumber, 1, Some(1), Some(lastUpdateDate)))

        result.left.value shouldBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error")
        )

        withClue("should send an audit event") {
          verify(auditGetRecordService).emitAuditGetRecordFailure(
            eqTo(AuditGetRecordRequest(eoriNumber, Some("2024-04-18T23:20:19.000Z"), Some(1), Some(1))),
            eqTo("2024-05-06T14:14:14.233Z"),
            eqTo("UNEXPECTED_ERROR"),
            eqTo(INTERNAL_SERVER_ERROR),
            eqTo(None)
          )(any)
        }
      }
    }
  }

  private def createEisErrorResponse =
    EisHttpErrorResponse(
      BAD_REQUEST,
      ErrorResponse(
        correlationId,
        "BAD_REQUEST",
        "BAD_REQUEST",
        Some(
          Seq(
            Error("INTERNAL_ERROR", "internal error 1", 6),
            Error("INTERNAL_ERROR", "internal error 2", 8)
          )
        )
      )
    )

}
