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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{CreateRecordConnector, EisHttpErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.CreateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import scala.concurrent.{ExecutionContext, Future}

class CreateRecordService @Inject() (
  connector: CreateRecordConnector,
  uuidService: UuidService,
  auditService: AuditService,
  dateTimeService: DateTimeService
)(implicit
  ec: ExecutionContext
) extends Logging {

  def createRecord(
    eori: String,
    request: CreateRecordRequest
  )(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, CreateOrUpdateRecordResponse]] = {
    val correlationId     = uuidService.uuid
    val requestedDateTime = dateTimeService.timestamp.asStringMilliSeconds
    val payload           = CreateRecordPayload(eori, request)

    connector
      .createRecord(payload, correlationId)
      .map {
        case Right(response) =>
          auditService.emitAuditCreateRecord(payload, requestedDateTime, "SUCCEEDED", OK, None, Some(response))
          Right(response)
        case Left(response)  =>
          val failureReason = response.errorResponse.errors.map { error =>
            error.map(e => e.message)
          }

          auditService.emitAuditCreateRecord(
            payload,
            requestedDateTime,
            response.errorResponse.code,
            response.httpStatus,
            failureReason
          )

          Left(response)

      }
      .recover { case ex: Throwable =>
        logger.error(
          s"""[CreateRecordService] - Error when creating records for Eori Number: $eori,
            correlationId: $correlationId, message: ${ex.getMessage}""",
          ex
        )

        auditService.emitAuditCreateRecord(
          payload,
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
