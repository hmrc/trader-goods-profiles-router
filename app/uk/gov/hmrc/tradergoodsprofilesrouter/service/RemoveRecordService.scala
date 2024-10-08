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
import play.api.Logging
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{EisHttpErrorResponse, RemoveRecordConnector}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.service.audit.AuditService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import scala.concurrent.{ExecutionContext, Future}

class RemoveRecordService @Inject() (
  connector: RemoveRecordConnector,
  uuidService: UuidService,
  auditService: AuditService,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends Logging {
  def removeRecord(eori: String, recordId: String, actorId: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[EisHttpErrorResponse, Int]] = {
    val correlationId     = uuidService.uuid
    val requestedDateTime = dateTimeService.timestamp.asStringMilliSeconds
    connector
      .removeRecord(eori, recordId, actorId, correlationId)
      .map {
        case Right(_) =>
          auditService.emitAuditRemoveRecord(eori, recordId, actorId, requestedDateTime, "SUCCEEDED", NO_CONTENT)
          Right(NO_CONTENT)

        case Left(response) =>
          val failureReason = response.errorResponse.errors.map { error =>
            error.map(e => e.message)
          }
          auditService.emitAuditRemoveRecord(
            eori,
            recordId,
            actorId,
            requestedDateTime,
            response.errorResponse.code,
            response.httpStatus,
            failureReason
          )
          Left(response)
      }
      .recover { case ex: Throwable =>
        logger.error(
          s"""[RemoveRecordService] - Error occurred while removing record for Eori Number: $eori, recordId: $recordId,
            actorId: $actorId, correlationId: $correlationId, message: ${ex.getMessage}""",
          ex
        )

        auditService.emitAuditRemoveRecord(
          eori,
          recordId,
          actorId,
          requestedDateTime,
          UnexpectedErrorCode,
          INTERNAL_SERVER_ERROR
        )
        Left(
          EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, UnexpectedErrorCode, ex.getMessage))
        )
      }
  }
}
