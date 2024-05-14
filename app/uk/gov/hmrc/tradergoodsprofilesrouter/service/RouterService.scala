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
import play.api.mvc.Result
import play.api.mvc.Results.{BadGateway, BadRequest, Forbidden, InternalServerError, MethodNotAllowed, NotFound, ServiceUnavailable}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{ErrorDetail, GetEisRecordsResponse, GoodsItemRecords}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[RouterServiceImpl])
trait RouterService {
  def fetchRecord(
    eori: String,
    recordId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): EitherT[Future, Result, GoodsItemRecords]

  def fetchRecords(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): EitherT[Future, Result, GetEisRecordsResponse]

  def createRecord(
    request: CreateRecordRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): EitherT[Future, Result, CreateRecordResponse]
}

class RouterServiceImpl @Inject() (eisConnector: EISConnector, uuidService: UuidService) extends RouterService {

  override def fetchRecord(eori: String, recordId: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, Result, GoodsItemRecords] = {
    val correlationId = uuidService.uuid
    EitherT(
      eisConnector
        .fetchRecord(eori, recordId, correlationId)
        .map(result => Right(result.goodsItemRecords.head))
        .recover {
          case UpstreamErrorResponse(message, BAD_REQUEST, _, _) =>
            Left(BadRequest(Json.toJson(determine400Error(correlationId, message))))
          case UpstreamErrorResponse(_, FORBIDDEN, _, _)         =>
            Left(
              Forbidden(
                Json.toJson(
                  ErrorResponse(
                    correlationId,
                    ApplicationConstants.ForbiddenCode,
                    ApplicationConstants.ForbiddenMessage
                  )
                )
              )
            )

          case UpstreamErrorResponse(_, NOT_FOUND, _, _)          =>
            Left(
              NotFound(
                Json.toJson(
                  ErrorResponse(
                    correlationId,
                    ApplicationConstants.NotFoundCode,
                    ApplicationConstants.NotFoundMessage
                  )
                )
              )
            )
          case UpstreamErrorResponse(_, METHOD_NOT_ALLOWED, _, _) =>
            Left(
              MethodNotAllowed(
                Json.toJson(
                  ErrorResponse(
                    correlationId,
                    ApplicationConstants.MethodNotAllowedCode,
                    ApplicationConstants.MethodNotAllowedMessage
                  )
                )
              )
            )

          case UpstreamErrorResponse(message, INTERNAL_SERVER_ERROR, _, _) =>
            Left(InternalServerError(Json.toJson(determine500Error(correlationId, message))))

          case NonFatal(e) =>
            logger.error(
              s"[RouterService] - Error getting record for eori number $eori and record ID $recordId, with message ${e.getMessage}",
              e
            )
            Left(
              InternalServerError(
                Json.toJson(
                  ErrorResponse(
                    correlationId,
                    ApplicationConstants.UnexpectedErrorCode,
                    ApplicationConstants.UnexpectedErrorMessage
                  )
                )
              )
            )
        }
    )
  }

  override def fetchRecords(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, Result, GetEisRecordsResponse] = {
    val correlationId = uuidService.uuid
    EitherT(
      eisConnector
        .fetchRecords(eori, correlationId, lastUpdatedDate, page, size)
        .map(result => Right(result))
        .recover {
          case UpstreamErrorResponse(message, BAD_REQUEST, _, _) =>
            Left(BadRequest(Json.toJson(determine400Error(correlationId, message))))
          case UpstreamErrorResponse(_, FORBIDDEN, _, _)         =>
            Left(
              Forbidden(
                Json.toJson(
                  ErrorResponse(
                    correlationId,
                    ApplicationConstants.ForbiddenCode,
                    ApplicationConstants.ForbiddenMessage
                  )
                )
              )
            )

          case UpstreamErrorResponse(_, NOT_FOUND, _, _)          =>
            Left(
              NotFound(
                Json.toJson(
                  ErrorResponse(
                    correlationId,
                    ApplicationConstants.NotFoundCode,
                    ApplicationConstants.NotFoundMessage
                  )
                )
              )
            )
          case UpstreamErrorResponse(_, METHOD_NOT_ALLOWED, _, _) =>
            Left(
              MethodNotAllowed(
                Json.toJson(
                  ErrorResponse(
                    correlationId,
                    ApplicationConstants.MethodNotAllowedCode,
                    ApplicationConstants.MethodNotAllowedMessage
                  )
                )
              )
            )

          case UpstreamErrorResponse(message, INTERNAL_SERVER_ERROR, _, _) =>
            Left(InternalServerError(Json.toJson(determine500Error(correlationId, message))))

          case NonFatal(e) =>
            logger.error(
              s"[RouterService] - Error getting records for eori number $eori,  with message ${e.getMessage}",
              e
            )
            Left(
              InternalServerError(
                Json.toJson(
                  ErrorResponse(
                    correlationId,
                    ApplicationConstants.UnexpectedErrorCode,
                    ApplicationConstants.UnexpectedErrorMessage
                  )
                )
              )
            )
        }
    )
  }

  override def createRecord(request: CreateRecordRequest)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, Result, CreateRecordResponse] = {
    val correlationId = uuidService.uuid
    EitherT(
      eisConnector
        .createRecord(request, correlationId)
        .map(result => Right(result))
        .recover {
          case UpstreamErrorResponse(message, BAD_REQUEST, _, _) =>
            Left(BadRequest(Json.toJson(determine400Error(correlationId, message))))

          case UpstreamErrorResponse(_, FORBIDDEN, _, _) =>
            Left(
              Forbidden(
                Json.toJson(
                  ErrorResponse(
                    correlationId,
                    ApplicationConstants.ForbiddenCode,
                    ApplicationConstants.ForbiddenMessage
                  )
                )
              )
            )

          case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
            Left(
              NotFound(
                Json.toJson(
                  ErrorResponse(
                    correlationId,
                    ApplicationConstants.NotFoundCode,
                    ApplicationConstants.NotFoundMessage
                  )
                )
              )
            )

          case UpstreamErrorResponse(_, METHOD_NOT_ALLOWED, _, _) =>
            Left(
              MethodNotAllowed(
                Json.toJson(
                  ErrorResponse(
                    correlationId,
                    ApplicationConstants.MethodNotAllowedCode,
                    ApplicationConstants.MethodNotAllowedMessage
                  )
                )
              )
            )

          case UpstreamErrorResponse(_, BAD_GATEWAY, _, _) =>
            Left(
              BadGateway(
                Json.toJson(
                  ErrorResponse(
                    correlationId,
                    ApplicationConstants.BadGatewayCode,
                    ApplicationConstants.BadGatewayMessage
                  )
                )
              )
            )

          case UpstreamErrorResponse(_, SERVICE_UNAVAILABLE, _, _) =>
            Left(
              ServiceUnavailable(
                Json.toJson(
                  ErrorResponse(
                    correlationId,
                    ApplicationConstants.ServiceUnavailableCode,
                    ApplicationConstants.ServiceUnavailableMessage
                  )
                )
              )
            )

          case UpstreamErrorResponse(message, INTERNAL_SERVER_ERROR, _, _) =>
            Left(InternalServerError(Json.toJson(determine500Error(correlationId, message))))

          case NonFatal(e) =>
            logger.error(
              s"[RouterService] - Error creating record for eori number ${request.eori} and actor ID ${request.actorId}, with message ${e.getMessage}",
              e
            )
            Left(
              InternalServerError(
                Json.toJson(
                  ErrorResponse(
                    correlationId,
                    ApplicationConstants.UnexpectedErrorCode,
                    ApplicationConstants.UnexpectedErrorMessage
                  )
                )
              )
            )
        }
    )
  }

  private def determine400Error(correlationId: String, message: String): ErrorResponse =
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

  private def determine500Error(correlationId: String, message: String): ErrorResponse =
    Json.parse(message).validate[ErrorDetail] match {
      case JsSuccess(detail, _) =>
        detail.errorCode match {
          case "200" | "201" =>
            ErrorResponse(
              correlationId,
              ApplicationConstants.InvalidOrEmptyPayloadCode,
              ApplicationConstants.InvalidOrEmptyPayloadMessage
            )
          case "400"         =>
            ErrorResponse(
              correlationId,
              ApplicationConstants.InternalErrorResponseCode,
              ApplicationConstants.InternalErrorResponseMessage
            )
          case "401"         =>
            ErrorResponse(
              correlationId,
              ApplicationConstants.UnauthorizedCode,
              ApplicationConstants.UnauthorizedMessage
            )
          case "404"         =>
            ErrorResponse(correlationId, ApplicationConstants.NotFoundCode, ApplicationConstants.NotFoundMessage)
          case "405"         =>
            ErrorResponse(
              correlationId,
              ApplicationConstants.MethodNotAllowedCode,
              ApplicationConstants.MethodNotAllowedMessage
            )
          case "500"         =>
            ErrorResponse(
              correlationId,
              ApplicationConstants.InternalServerErrorCode,
              ApplicationConstants.InternalServerErrorMessage
            )
          case "502"         =>
            ErrorResponse(
              correlationId,
              ApplicationConstants.BadGatewayCode,
              ApplicationConstants.BadGatewayMessage
            )
          case "503"         =>
            ErrorResponse(
              correlationId,
              ApplicationConstants.ServiceUnavailableCode,
              ApplicationConstants.ServiceUnavailableMessage
            )
          case _             =>
            ErrorResponse(correlationId, ApplicationConstants.UnknownCode, ApplicationConstants.UnknownMessage)
        }
      case JsError(_)           =>
        ErrorResponse(
          correlationId,
          ApplicationConstants.UnexpectedErrorCode,
          ApplicationConstants.UnexpectedErrorMessage
        )
    }

  private def setBadRequestResponse(correlationId: String, detail: ErrorDetail): ErrorResponse =
    ErrorResponse(
      correlationId,
      ApplicationConstants.BadRequestCode,
      ApplicationConstants.BadRequestMessage,
      detail.sourceFaultDetail.map { sfd =>
        sfd.detail.get.map(detail => parseFaultDetail(detail, correlationId))
      }
    )

  private def parseFaultDetail(rawDetail: String, correlationId: String): Error = {
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
          case "008" =>
            Error(ApplicationConstants.InvalidRequestParameterCode, ApplicationConstants.InvalidOrMissingActorId)
          case "009" =>
            Error(ApplicationConstants.InvalidRequestParameterCode, ApplicationConstants.InvalidOrMissingTraderRef)
          case "010" =>
            Error(ApplicationConstants.InvalidRequestParameterCode, ApplicationConstants.TraderRefIsNotUnique)
          case "011" =>
            Error(ApplicationConstants.InvalidRequestParameterCode, ApplicationConstants.InvalidOrMissingComcode)
          case "012" =>
            Error(
              ApplicationConstants.InvalidRequestParameterCode,
              ApplicationConstants.InvalidOrMissingGoodsDescription
            )
          case "013" =>
            Error(
              ApplicationConstants.InvalidRequestParameterCode,
              ApplicationConstants.InvalidOrMissingCountryOfOrigin
            )
          case "014" =>
            Error(ApplicationConstants.InvalidRequestParameterCode, ApplicationConstants.InvalidOrMissingCategory)
          case "015" =>
            Error(ApplicationConstants.InvalidRequestParameterCode, ApplicationConstants.InvalidOrMissingAssessmentId)
          case "016" =>
            Error(
              ApplicationConstants.InvalidRequestParameterCode,
              ApplicationConstants.InvalidAssessmentPrimaryCategory
            )
          case "017" =>
            Error(
              ApplicationConstants.InvalidRequestParameterCode,
              ApplicationConstants.InvalidAssessmentPrimaryCategoryConditionType
            )
          case "018" =>
            Error(
              ApplicationConstants.InvalidRequestParameterCode,
              ApplicationConstants.InvalidAssessmentPrimaryCategoryConditionId
            )
          case "019" =>
            Error(
              ApplicationConstants.InvalidRequestParameterCode,
              ApplicationConstants.InvalidAssessmentPrimaryCategoryConditionDescription
            )
          case "020" =>
            Error(
              ApplicationConstants.InvalidRequestParameterCode,
              ApplicationConstants.InvalidAssessmentPrimaryCategoryConditionTraderText
            )
          case "021" =>
            Error(
              ApplicationConstants.InvalidRequestParameterCode,
              ApplicationConstants.InvalidOrMissingSupplementaryUnit
            )
          case "022" =>
            Error(
              ApplicationConstants.InvalidRequestParameterCode,
              ApplicationConstants.InvalidOrMissingMeasurementUnit
            )
          case "023" =>
            Error(
              ApplicationConstants.InvalidRequestParameterCode,
              ApplicationConstants.InvalidOrMissingComcodeEffectiveFromDate
            )
          case "024" =>
            Error(
              ApplicationConstants.InvalidRequestParameterCode,
              ApplicationConstants.InvalidOrMissingComcodeEffectiveToDate
            )
          case "025" =>
            Error(ApplicationConstants.InvalidRequestParameterCode, ApplicationConstants.InvalidRecordId)
          case "026" =>
            Error(ApplicationConstants.InvalidRequestParameterCode, ApplicationConstants.RecordIdDoesNotExists)
          case "028" =>
            Error(ApplicationConstants.InvalidRequestParameterCode, ApplicationConstants.InvalidLastUpdatedDate)
          case "029" =>
            Error(ApplicationConstants.InvalidRequestParameterCode, ApplicationConstants.InvalidPage)
          case "030" =>
            Error(ApplicationConstants.InvalidRequestParameterCode, ApplicationConstants.InvalidSize)
          case _     => Error(ApplicationConstants.UnexpectedErrorCode, ApplicationConstants.UnexpectedErrorMessage)
        }
      case _              =>
        throw new IllegalArgumentException(s"Unable to parse fault detail for correlation Id: $correlationId")
    }
  }
}
