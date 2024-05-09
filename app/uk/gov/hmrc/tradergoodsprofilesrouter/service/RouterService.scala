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

import cats.data.EitherT
import com.google.inject.{ImplementedBy, Inject}
import play.api.http.Status._
import play.api.libs.Files.logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{ErrorDetail, GoodsItemRecords}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse, RouterError}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[RouterServiceImpl])
trait RouterService {
  def fetchRecord(
    eori: String,
    recordId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): EitherT[Future, RouterError, GoodsItemRecords]
}

class RouterServiceImpl @Inject() (eisConnector: EISConnector, uuidService: UuidService) extends RouterService {

  override def fetchRecord(eori: String, recordId: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, RouterError, GoodsItemRecords] = {
    val correlationId = uuidService.uuid
    EitherT(
      eisConnector
        .fetchRecord(eori, recordId, correlationId)
        .map(result => Right(result.goodsItemRecords.head))
        .recover {
          case UpstreamErrorResponse(message, BAD_REQUEST, _, _) =>
            Left(RouterError(BAD_REQUEST, determine400Error(correlationId, message)))

          case UpstreamErrorResponse(_, FORBIDDEN, _, _)                   =>
            Left(
              RouterError(
                FORBIDDEN,
                ErrorResponse(
                  correlationId,
                  ApplicationConstants.ForbiddenCode,
                  ApplicationConstants.ForbiddenMessage
                )
              )
            )
          case UpstreamErrorResponse(_, NOT_FOUND, _, _)                   =>
            Left(
              RouterError(
                NOT_FOUND,
                ErrorResponse(
                  correlationId,
                  ApplicationConstants.NotFoundCode,
                  ApplicationConstants.NotFoundMessage
                )
              )
            )
          case UpstreamErrorResponse(_, METHOD_NOT_ALLOWED, _, _)          =>
            Left(
              RouterError(
                METHOD_NOT_ALLOWED,
                ErrorResponse(
                  correlationId,
                  ApplicationConstants.MethodNotAllowedCode,
                  ApplicationConstants.MethodNotAllowedMessage
                )
              )
            )
          case UpstreamErrorResponse(message, INTERNAL_SERVER_ERROR, _, _) =>
            Left(determine500Error(correlationId, message))
          case NonFatal(e)                                                 =>
            logger.error(
              s"[RouterService] - Error getting record for eori number $eori and record ID $recordId, with message ${e.getMessage}",
              e
            )
            Left(
              RouterError(
                INTERNAL_SERVER_ERROR,
                ErrorResponse(
                  correlationId,
                  ApplicationConstants.UnexpectedErrorCode,
                  ApplicationConstants.UnexpectedErrorMessage
                )
              )
            )
        }
    )
  }

  def determine400Error(correlationId: String, message: String): ErrorResponse =
    Json.parse(message).validate[ErrorDetail] match {
      case JsSuccess(detail, _) =>
        setBadRequestResponse(correlationId, detail)
      case JsError(_)           =>
        ErrorResponse(
          correlationId,
          ApplicationConstants.UnexpectedErrorCode,
          ApplicationConstants.UnexpectedErrorMessage
        )
    }

  def determine500Error(correlationId: String, message: String): RouterError =
    Json.parse(message).validate[ErrorDetail] match {
      case JsSuccess(detail, _) =>
        detail.errorCode match {
          case "200" =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(
                correlationId,
                ApplicationConstants.InvalidOrEmptyPayloadCode,
                ApplicationConstants.InvalidOrEmptyPayloadMessage
              )
            )
          case "400" =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(
                correlationId,
                ApplicationConstants.InternalErrorResponseCode,
                ApplicationConstants.InternalErrorResponseMessage
              )
            )
          case "401" =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(
                correlationId,
                ApplicationConstants.UnauthorizedCode,
                ApplicationConstants.UnauthorizedMessage
              )
            )
          case "404" =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(correlationId, ApplicationConstants.NotFoundCode, ApplicationConstants.NotFoundMessage)
            )
          case "405" =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(
                correlationId,
                ApplicationConstants.MethodNotAllowedCode,
                ApplicationConstants.MethodNotAllowedMessage
              )
            )
          case "500" =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(
                correlationId,
                ApplicationConstants.InternalServerErrorCode,
                ApplicationConstants.InternalServerErrorMessage
              )
            )
          case "502" =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(
                correlationId,
                ApplicationConstants.BadGatewayCode,
                ApplicationConstants.BadGatewayMessage
              )
            )
          case "503" =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(
                correlationId,
                ApplicationConstants.ServiceUnavailableCode,
                ApplicationConstants.ServiceUnavailableMessage
              )
            )
          case _     =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(correlationId, ApplicationConstants.UnknownCode, ApplicationConstants.UnknownMessage)
            )
        }
      case JsError(_)           =>
        RouterError(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(
            correlationId,
            ApplicationConstants.UnexpectedErrorCode,
            ApplicationConstants.UnexpectedErrorMessage
          )
        )
    }

  def setBadRequestResponse(correlationId: String, detail: ErrorDetail): ErrorResponse =
    ErrorResponse(
      correlationId,
      ApplicationConstants.BadRequestCode,
      ApplicationConstants.BadRequestMessage,
      detail.sourceFaultDetail.map { sfd =>
        sfd.detail.get.map(parseFaultDetail)
      }
    )

  def parseFaultDetail(rawDetail: String): Error = {
    val regex = """error:\s*(\d+),\s*message:\s*(.*)""".r

    rawDetail match {
      case regex(code, _) =>
        code match {
          case "006" =>
            Error(
              ApplicationConstants.InvalidRequestParameterCode,
              ApplicationConstants.InvalidOrMissingEori
            )
          case "007" =>
            Error(ApplicationConstants.InvalidRequestParameterCode, ApplicationConstants.EoriDoesNotExists)
          case "025" =>
            Error(ApplicationConstants.InvalidRequestParameterCode, ApplicationConstants.InvalidRecordId)
          case "026" =>
            Error(ApplicationConstants.InvalidRequestParameterCode, ApplicationConstants.RecordIdDoesNotExists)
          case _     => Error(ApplicationConstants.UnexpectedErrorCode, ApplicationConstants.UnexpectedErrorMessage)
        }
      case _              =>
        throw new IllegalArgumentException(s"Unable to parse fault detail: $rawDetail")
    }
  }
}
