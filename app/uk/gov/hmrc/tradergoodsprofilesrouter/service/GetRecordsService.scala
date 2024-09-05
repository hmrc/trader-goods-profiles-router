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

import com.google.inject.Inject
import org.apache.pekko.Done
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{EisHttpErrorResponse, GetRecordsConnector}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.request.AuditGetRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.{GetRecordsResponse, GoodsItemRecords}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.service.audit.AuditGetRecordService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class GetRecordsService @Inject() (
  eisConnector: GetRecordsConnector,
  uuidService: UuidService,
  dateTimeService: DateTimeService,
  auditGetRecordService: AuditGetRecordService
)(implicit
  ec: ExecutionContext
) extends Logging {

  def fetchRecord(eori: String, recordId: String, url: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[EisHttpErrorResponse, GoodsItemRecords]] = {
    val correlationId: String = uuidService.uuid
    val requestDateTime       = dateTimeService.timestamp.asStringMilliSeconds
    val request               = AuditGetRecordRequest(eori = eori, recordId = Some(recordId))

    eisConnector
      .fetchRecord(eori, recordId, correlationId, url)
      .map {
        case Right(response)     =>
          auditGetRecordService.emitAuditGetRecordSucceeded(request, requestDateTime, response)
          Right(GoodsItemRecords(response.goodsItemRecords.head))
        case Left(errorResponse) =>
          logger.warn(s"""[GetRecordsService] - Error when retrieving record for eori: $eori,
             correlationId: $correlationId, status: ${errorResponse.httpStatus},
             body: ${Json.toJson(errorResponse.errorResponse)}""".stripMargin)

          sendAuditForFailure(requestDateTime, errorResponse, request)
          Left(errorResponse)
      }
      .recover { case ex: Throwable =>
        logger.error(
          s"""[GetRecordsService] - Exception thrown when fetching a single record for Eori Number: $eori,
            s"recordId: $recordId, correlationId: $correlationId, message: ${ex.getMessage}""",
          ex
        )

        val error =
          EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, UnexpectedErrorCode, ex.getMessage))
        sendAuditForFailure(requestDateTime, error, request)
        Left(error)
      }

  }

  def fetchRecords(
    eori: String,
    size: Int,
    page: Option[Int] = None,
    lastUpdatedDate: Option[Instant] = None
  )(implicit
    hc: HeaderCarrier
  ): Future[Either[EisHttpErrorResponse, GetRecordsResponse]] = {
    val correlationId: String = uuidService.uuid
    val requestDateTime       = dateTimeService.timestamp.asStringMilliSeconds
    val request               = AuditGetRecordRequest(
      eori,
      lastUpdatedDate.map(_.asStringMilliSeconds),
      page,
      Some(size)
    )

    eisConnector
      .fetchRecords(eori, correlationId, size, page, lastUpdatedDate)
      .map {
        case Right(response)     =>
          auditGetRecordService.emitAuditGetRecordSucceeded(request, requestDateTime, response)
          Right(GetRecordsResponse(response))
        case Left(errorResponse) =>
          logger.warn(s"""[GetRecordsService] - Error when retieving multiple record for eori: $eori,
             correlationId: $correlationId, status: ${errorResponse.httpStatus},
             body: ${Json.toJson(errorResponse.errorResponse)}""".stripMargin)

          sendAuditForFailure(requestDateTime, errorResponse, request)
          Left(errorResponse)
      }
      .recover { case ex: Throwable =>
        logger.error(
          s"""[GetRecordsService] - Exception thrown when fetching multiple records for eori: $eori,
            s"correlationId: $correlationId, message: ${ex.getMessage}""",
          ex
        )

        val error =
          EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, UnexpectedErrorCode, ex.getMessage))
        sendAuditForFailure(requestDateTime, error, request)
        Left(error)
      }
  }

  private def sendAuditForFailure(
    requestDateTime: String,
    error: EisHttpErrorResponse,
    request: AuditGetRecordRequest
  )(implicit hc: HeaderCarrier): Future[Done] =
    auditGetRecordService.emitAuditGetRecordFailure(
      request,
      requestDateTime,
      error.errorResponse.code,
      error.httpStatus,
      error.errorResponse.errors.map(_.map(_.message))
    )
}
