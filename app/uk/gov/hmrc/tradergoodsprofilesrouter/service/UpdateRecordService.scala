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

import play.api.Logging
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{EisHttpErrorResponse, UpdateRecordConnector}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.CreateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.{CreateRecordRequest, UpdateRecordRequest}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.payloads.UpdateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateRecordService @Inject() (
  connector: UpdateRecordConnector,
  uuidService: UuidService,
  auditService: AuditService,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends Logging {

  def patchRecord(
    eori: String,
    recordId: String,
    request: UpdateRecordRequest
  )(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, CreateOrUpdateRecordResponse]] = {
    val correlationId     = uuidService.uuid
    val payload           = UpdateRecordPayload(eori, recordId, request)
    val requestedDateTime = dateTimeService.timestamp.asStringMilliSeconds

    connector
      .updateRecord(payload, correlationId)
      .map {
        case Right(response) =>
          val updateRecordResponse = CreateOrUpdateRecordResponse(response)
          auditService.emitAuditUpdateRecord(
            payload,
            requestedDateTime,
            "SUCCEEDED",
            OK,
            None,
            Some(updateRecordResponse)
          )
          Right(updateRecordResponse)
        case Left(response)  =>
          val failureReason = response.errorResponse.errors.map { error =>
            error.map(e => e.message)
          }

          auditService.emitAuditUpdateRecord(
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
          s"""[UpdateRecordService] - Error when updating records for Eori Number: $eori,
            s"correlationId: $correlationId, message: ${ex.getMessage}""",
          ex
        )

        auditService.emitAuditUpdateRecord(
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

  def putRecord(
    eori: String,
    recordId: String,
    request: CreateRecordRequest
  )(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, CreateOrUpdateRecordResponse]] = {
    val correlationId     = uuidService.uuid
    val payload           = CreateRecordPayload(eori, request)
    val requestedDateTime = dateTimeService.timestamp.asStringMilliSeconds

    connector
      .put(payload, correlationId)
      .map {
        case Right(response) =>
          val updateRecordResponse = CreateOrUpdateRecordResponse(response)

          auditService.emitAuditUpdateRecord(
            createUpdatePayloadFromCreatePayload(payload, recordId),
            requestedDateTime,
            "SUCCEEDED",
            OK,
            None,
            Some(updateRecordResponse)
          )

          Right(updateRecordResponse)
        case Left(response)  =>
          val failureReason = response.errorResponse.errors.map { error =>
            error.map(e => e.message)
          }

          auditService.emitAuditUpdateRecord(
            createUpdatePayloadFromCreatePayload(payload, recordId),
            requestedDateTime,
            response.errorResponse.code,
            response.httpStatus,
            failureReason
          )

          Left(response)
      }
      .recover { case ex: Throwable =>
        logger.error(
          s"""[UpdateRecordService] - Error when updating records for Eori Number: $eori,
            s"correlationId: $correlationId, message: ${ex.getMessage}""",
          ex
        )

        auditService.emitAuditUpdateRecord(
          createUpdatePayloadFromCreatePayload(payload, recordId),
          requestedDateTime,
          UnexpectedErrorCode,
          INTERNAL_SERVER_ERROR
        )

        Left(
          EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, UnexpectedErrorCode, ex.getMessage))
        )
      }
  }

  private def createUpdatePayloadFromCreatePayload(
    payload: CreateRecordPayload,
    recordId: String
  ): UpdateRecordPayload =
    UpdateRecordPayload(
      eori = payload.eori,
      recordId = recordId,
      actorId = payload.actorId,
      traderRef = Some(payload.traderRef),
      comcode = Some(payload.comcode),
      goodsDescription = Some(payload.goodsDescription),
      countryOfOrigin = Some(payload.countryOfOrigin),
      category = payload.category,
      assessments = payload.assessments,
      supplementaryUnit = payload.supplementaryUnit,
      measurementUnit = payload.measurementUnit,
      comcodeEffectiveFromDate = Some(payload.comcodeEffectiveFromDate),
      comcodeEffectiveToDate = payload.comcodeEffectiveToDate
    )
}
