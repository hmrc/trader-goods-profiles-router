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

import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, METHOD_NOT_ALLOWED, NOT_FOUND, SERVICE_UNAVAILABLE}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.Result
import play.api.mvc.Results.{BadGateway, BadRequest, Forbidden, InternalServerError, MethodNotAllowed, NotFound, ServiceUnavailable}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.ErrorDetail
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.Error._
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._

trait EisHttpErrorHandler {
  def handleErrorResponse(httpResponse: HttpResponse, correlationId: String): Result =
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

  private def determine500Error(correlationId: String, message: String): ErrorResponse =
    Json.parse(message).validate[ErrorDetail] match {
      case JsSuccess(detail, _) =>
        detail.errorCode match {
          //todo: 200 is only required for GetRecord and 201 only for create. We may
          // want to refactor this to only use the right code for the right request
          // as this is assuming that GetRecord also can get a 201 and CreateRecord can also get
          // a 200
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

  private def parseFaultDetail(rawDetail: String, correlationId: String) = {
    val regex = """error:\s*(\w+),\s*message:\s*(.*)""".r
    println(rawDetail)
    rawDetail match {
      case regex(code, _) =>
        code match {
          case InvalidOrMissingEoriCode                                 =>
            invalidRequestParameterError(
              InvalidOrMissingEori,
              code.toInt
            )
          case EoriDoesNotExistsCode                                    =>
            invalidRequestParameterError(EoriDoesNotExists, code.toInt)
          case InvalidOrMissingActorIdCode                              =>
            invalidRequestParameterError(InvalidOrMissingActorId, code.toInt)
          case InvalidOrMissingTraderRefCode                            =>
            invalidRequestParameterError(InvalidOrMissingTraderRef, code.toInt)
          case TraderRefIsNotUniqueCode                                 =>
            invalidRequestParameterError(TraderRefIsNotUnique, code.toInt)
          case InvalidOrMissingComcodeCode                              =>
            invalidRequestParameterError(InvalidOrMissingComcode, code.toInt)
          case InvalidOrMissingGoodsDescriptionCode                     =>
            invalidRequestParameterError(
              InvalidOrMissingGoodsDescription,
              code.toInt
            )
          case InvalidOrMissingCountryOfOriginCode                      =>
            invalidRequestParameterError(
              InvalidOrMissingCountryOfOrigin,
              code.toInt
            )
          case InvalidOrMissingCategoryCode                             =>
            invalidRequestParameterError(InvalidOrMissingCategory, code.toInt)
          case InvalidOrMissingAssessmentIdCode                         =>
            invalidRequestParameterError(
              InvalidOrMissingAssessmentId,
              code.toInt
            )
          case InvalidAssessmentPrimaryCategoryCode                     =>
            invalidRequestParameterError(
              InvalidAssessmentPrimaryCategory,
              code.toInt
            )
          case InvalidAssessmentPrimaryCategoryConditionTypeCode        =>
            invalidRequestParameterError(
              InvalidAssessmentPrimaryCategoryConditionType,
              code.toInt
            )
          case InvalidAssessmentPrimaryCategoryConditionIdCode          =>
            invalidRequestParameterError(
              InvalidAssessmentPrimaryCategoryConditionId,
              code.toInt
            )
          case InvalidAssessmentPrimaryCategoryConditionDescriptionCode =>
            invalidRequestParameterError(
              InvalidAssessmentPrimaryCategoryConditionDescription,
              code.toInt
            )
          case InvalidAssessmentPrimaryCategoryConditionTraderTextCode  =>
            invalidRequestParameterError(
              InvalidAssessmentPrimaryCategoryConditionTraderText,
              code.toInt
            )
          case InvalidOrMissingSupplementaryUnitCode                    =>
            invalidRequestParameterError(
              InvalidOrMissingSupplementaryUnit,
              code.toInt
            )
          case InvalidOrMissingMeasurementUnitCode                      =>
            invalidRequestParameterError(
              InvalidOrMissingMeasurementUnit,
              code.toInt
            )
          case InvalidOrMissingComcodeEffectiveFromDateCode             =>
            invalidRequestParameterError(
              InvalidOrMissingComcodeEffectiveFromDate,
              code.toInt
            )
          case InvalidOrMissingComcodeEffectiveToDateCode               =>
            invalidRequestParameterError(
              InvalidOrMissingComcodeEffectiveToDate,
              code.toInt
            )
          case InvalidRecordIdCode                                      =>
            invalidRequestParameterError(InvalidRecordId, code.toInt)
          case RecordIdDoesNotExistsCode                                =>
            invalidRequestParameterError(RecordIdDoesNotExists, code.toInt)
          case InvalidLastUpdatedDateCode                               =>
            invalidRequestParameterError(InvalidLastUpdatedDate, code.toInt)
          case InvalidPageCode                                          =>
            invalidRequestParameterError(InvalidPage, code.toInt)
          case InvalidSizeCode                                          =>
            invalidRequestParameterError(InvalidSize, code.toInt)
          case AccreditationRequestInProgressCode                       =>
            invalidRequestParameterError(AccreditationRequestInProgressMessage, code.toInt)
          case RecordRemovedAndCanNotBeUpdatedCode                      =>
            invalidRequestParameterError(RecordRemovedAndCanNotBeUpdatedMessage, code.toInt)
          case InvalidOrMissingCorrelationIdCode                        =>
            invalidRequestParameterError(InvalidOrMissingCorrelationID, 1001)
          case InvalidOrMissingRequestDateCode                          =>
            invalidRequestParameterError(InvalidOrMissingRequestDate, 1002)
          case InvalidOrMissingForwardedHostCode                        =>
            invalidRequestParameterError(InvalidOrMissingForwardedHost, 1003)
          case InvalidOrMissingContentTypeCode                          =>
            invalidRequestParameterError(InvalidOrMissingContentType, 1004)
          case InvalidOrMissingAcceptCode                               =>
            invalidRequestParameterError(InvalidOrMissingAccept, 1005)
          case InvalidOrMissingReceiptDateCode                          =>
            invalidRequestParameterError(InvalidOrMissingReceiptDate, 1006)
          case InvalidOrMissingTraderEORICode                           =>
            invalidRequestParameterError(InvalidOrMissingTraderEORI, 1007)
          case InvalidOrMissingRequestorNameCode                        =>
            invalidRequestParameterError(InvalidOrMissingRequestorName, 1008)
          case InvalidOrMissingRequestorEmailCode                       =>
            invalidRequestParameterError(InvalidOrMissingRequestorEmail, 1009)
          case InvalidOrMissingUkimsAuthorisationCode                   =>
            invalidRequestParameterError(InvalidOrMissingUkimsAuthorisation, 1010)
          case InvalidOrMissingGoodsItemsCode                           =>
            invalidRequestParameterError(InvalidOrMissingGoodsItems, 1011)
          case InvalidOrMissingPublicRecordIDCode                       =>
            invalidRequestParameterError(InvalidOrMissingPublicRecordID, 1012)
          case InvalidOrMissingTraderReferenceCode                      =>
            invalidRequestParameterError(InvalidOrMissingTraderReference, 1013)
          case InvalidOrMissinggoodsDescriptionCode                     =>
            invalidRequestParameterError(InvalidOrMissingGoodsDescription, 1014)
          case InvalidOrMissingCommodityCodeCode                        =>
            invalidRequestParameterError(InvalidOrMissingCommodityCode, 1015)

          case _ => unexpectedError("Unrecognised error number", code.toInt)
        }
      case _              =>
        throw new IllegalArgumentException(s"Unable to parse fault detail for correlation Id: $correlationId")
    }
  }
}
