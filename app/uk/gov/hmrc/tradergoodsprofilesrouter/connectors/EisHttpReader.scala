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

package uk.gov.hmrc.tradergoodsprofilesrouter.connectors

import play.api.http.Status._
import play.api.libs.Files.logger
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.{BadGateway, BadRequest, Forbidden, InternalServerError, MethodNotAllowed, NotFound, ServiceUnavailable}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.ErrorDetail
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._

import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success, Try}

object EisHttpReader {

  case class HttpReader[T](correlationId: String)(implicit reads: Reads[T], tt: TypeTag[T])
      extends HttpReads[Either[Result, T]] {
    override def read(method: String, url: String, response: HttpResponse): Either[Result, T] =
      response match {
        case response if isSuccessful(response.status) =>
          Right(jsonAs[T](response))
        case response                                  => Left(handleErrorResponse(response)(correlationId))
      }
  }

  private def handleErrorResponse(httpResponse: HttpResponse)(implicit correlationId: String): Result =
    httpResponse.status match {

      case BAD_REQUEST =>
        BadRequest(Json.toJson(determine400Error(correlationId, httpResponse.body)))
      case FORBIDDEN   =>
        Forbidden(
          Json.toJson(
            ErrorResponse(
              correlationId,
              ForbiddenCode,
              ForbiddenMessage
            )
          )
        )
      case NOT_FOUND   =>
        NotFound(
          Json.toJson(
            ErrorResponse(
              correlationId,
              NotFoundCode,
              NotFoundMessage
            )
          )
        )

      case METHOD_NOT_ALLOWED =>
        MethodNotAllowed(
          Json.toJson(
            ErrorResponse(
              correlationId,
              MethodNotAllowedCode,
              MethodNotAllowedMessage
            )
          )
        )

      case BAD_GATEWAY =>
        BadGateway(
          Json.toJson(
            ErrorResponse(
              correlationId,
              BadGatewayCode,
              BadGatewayMessage
            )
          )
        )

      case SERVICE_UNAVAILABLE =>
        ServiceUnavailable(
          Json.toJson(
            ErrorResponse(
              correlationId,
              ServiceUnavailableCode,
              ServiceUnavailableMessage
            )
          )
        )

      case INTERNAL_SERVER_ERROR =>
        InternalServerError(Json.toJson(determine500Error(correlationId, httpResponse.body)))
      case _                     =>
        InternalServerError(
          Json.toJson(
            ErrorResponse(
              correlationId,
              UnexpectedErrorCode,
              UnexpectedErrorMessage
            )
          )
        )
    }

  def jsonAs[T](response: HttpResponse)(implicit reads: Reads[T], tt: TypeTag[T]): T =
    Try(Json.parse(response.body)) match {
      case Success(value)     =>
        value
          .validate[T]
          .map(result => result)
          .recoverTotal { error: JsError =>
            logger.error(
              s"[EisConnector] - Failed to validate or parse JSON body of type: ${typeOf[T]}",
              error
            )
            throw new RuntimeException(JsResult.Exception(error))
          }
      case Failure(exception) =>
        logger.error(
          s"[EisConnector] - Response body could not be parsed as JSON, body: ${response.body}",
          exception
        )
        throw new RuntimeException(s"Response body could not be read: ${response.body}")
    }

  private def determine500Error(correlationId: String, message: String): ErrorResponse =
    Json.parse(message).validate[ErrorDetail] match {
      case JsSuccess(detail, _) =>
        detail.errorCode match {
          case "200" | "201" =>
            ErrorResponse(
              correlationId,
              InvalidOrEmptyPayloadCode,
              InvalidOrEmptyPayloadMessage
            )
          case "400"         =>
            ErrorResponse(
              correlationId,
              InternalErrorResponseCode,
              InternalErrorResponseMessage
            )
          case "401"         =>
            ErrorResponse(
              correlationId,
              UnauthorizedCode,
              UnauthorizedMessage
            )
          case "404"         =>
            ErrorResponse(correlationId, NotFoundCode, NotFoundMessage)
          case "405"         =>
            ErrorResponse(
              correlationId,
              MethodNotAllowedCode,
              MethodNotAllowedMessage
            )
          case "500"         =>
            ErrorResponse(
              correlationId,
              InternalServerErrorCode,
              InternalServerErrorMessage
            )
          case "502"         =>
            ErrorResponse(
              correlationId,
              BadGatewayCode,
              BadGatewayMessage
            )
          case "503"         =>
            ErrorResponse(
              correlationId,
              ServiceUnavailableCode,
              ServiceUnavailableMessage
            )
          case _             =>
            ErrorResponse(correlationId, UnknownCode, UnknownMessage)
        }
      case JsError(_)           =>
        ErrorResponse(
          correlationId,
          UnexpectedErrorCode,
          UnexpectedErrorMessage
        )
    }

  private def determine400Error(correlationId: String, message: String): ErrorResponse =
    Json.parse(message).validate[ErrorDetail] match {
      case JsSuccess(detail, _) =>
        setBadRequestResponse(correlationId, detail)
      case JsError(_)           =>
        ErrorResponse(
          correlationId,
          UnexpectedErrorCode,
          UnexpectedErrorMessage
        )
    }

  private def setBadRequestResponse(correlationId: String, detail: ErrorDetail): ErrorResponse =
    ErrorResponse(
      correlationId,
      BadRequestCode,
      BadRequestMessage,
      detail.sourceFaultDetail.map { sfd =>
        //todo: do not use get on an option as the option can be None and then this will crash
        sfd.detail.get.map(detail => parseFaultDetail(detail, correlationId))
      }
    )

  private def parseFaultDetail(rawDetail: String, correlationId: String): Error = {
    val regex = """error:\s*(\d+),\s*message:\s*(.*)""".r

    rawDetail match {
      case regex(code, _) =>
        code match {
          case InvalidOrMissingEoriCode                                 =>
            Error(
              InvalidOrMissingEoriCode,
              InvalidOrMissingEori
            )
          case EoriDoesNotExistsCode                                    =>
            Error(EoriDoesNotExistsCode, EoriDoesNotExists)
          case InvalidOrMissingActorIdCode                              =>
            Error(InvalidOrMissingActorIdCode, InvalidOrMissingActorId)
          case InvalidOrMissingTraderRefCode                            =>
            Error(InvalidOrMissingTraderRefCode, InvalidOrMissingTraderRef)
          case TraderRefIsNotUniqueCode                                 =>
            Error(TraderRefIsNotUniqueCode, TraderRefIsNotUnique)
          case InvalidOrMissingComcodeCode                              =>
            Error(InvalidOrMissingComcodeCode, InvalidOrMissingComcode)
          case InvalidOrMissingGoodsDescriptionCode                     =>
            Error(
              InvalidOrMissingGoodsDescriptionCode,
              InvalidOrMissingGoodsDescription
            )
          case InvalidOrMissingCountryOfOriginCode                      =>
            Error(
              InvalidOrMissingCountryOfOriginCode,
              InvalidOrMissingCountryOfOrigin
            )
          case InvalidOrMissingCategoryCode                             =>
            Error(InvalidOrMissingCategoryCode, InvalidOrMissingCategory)
          case InvalidOrMissingAssessmentIdCode                         =>
            Error(
              InvalidOrMissingAssessmentIdCode,
              InvalidOrMissingAssessmentId
            )
          case InvalidAssessmentPrimaryCategoryCode                     =>
            Error(
              InvalidAssessmentPrimaryCategoryCode,
              InvalidAssessmentPrimaryCategory
            )
          case InvalidAssessmentPrimaryCategoryConditionTypeCode        =>
            Error(
              InvalidAssessmentPrimaryCategoryConditionTypeCode,
              InvalidAssessmentPrimaryCategoryConditionType
            )
          case InvalidAssessmentPrimaryCategoryConditionIdCode          =>
            Error(
              InvalidAssessmentPrimaryCategoryConditionIdCode,
              InvalidAssessmentPrimaryCategoryConditionId
            )
          case InvalidAssessmentPrimaryCategoryConditionDescriptionCode =>
            Error(
              InvalidAssessmentPrimaryCategoryConditionDescriptionCode,
              InvalidAssessmentPrimaryCategoryConditionDescription
            )
          case InvalidAssessmentPrimaryCategoryConditionTraderTextCode  =>
            Error(
              InvalidAssessmentPrimaryCategoryConditionTraderTextCode,
              InvalidAssessmentPrimaryCategoryConditionTraderText
            )
          case InvalidOrMissingSupplementaryUnitCode                    =>
            Error(
              InvalidOrMissingSupplementaryUnitCode,
              InvalidOrMissingSupplementaryUnit
            )
          case InvalidOrMissingMeasurementUnitCode                      =>
            Error(
              InvalidOrMissingMeasurementUnitCode,
              InvalidOrMissingMeasurementUnit
            )
          case InvalidOrMissingComcodeEffectiveFromDateCode             =>
            Error(
              InvalidOrMissingComcodeEffectiveFromDateCode,
              InvalidOrMissingComcodeEffectiveFromDate
            )
          case InvalidOrMissingComcodeEffectiveToDateCode               =>
            Error(
              InvalidOrMissingComcodeEffectiveToDateCode,
              InvalidOrMissingComcodeEffectiveToDate
            )
          case InvalidRecordIdCode                                      =>
            Error(InvalidRecordIdCode, InvalidRecordId)
          case RecordIdDoesNotExistsCode                                =>
            Error(RecordIdDoesNotExistsCode, RecordIdDoesNotExists)
          case InvalidLastUpdatedDateCode                               =>
            Error(InvalidLastUpdatedDateCode, InvalidLastUpdatedDate)
          case InvalidPageCode                                          =>
            Error(InvalidPageCode, InvalidPage)
          case InvalidSizeCode                                          =>
            Error(InvalidSizeCode, InvalidSize)
          case _                                                        => Error(UnexpectedErrorCode, UnexpectedErrorMessage)
        }
      case _              =>
        throw new IllegalArgumentException(s"Unable to parse fault detail for correlation Id: $correlationId")
    }
  }

}
