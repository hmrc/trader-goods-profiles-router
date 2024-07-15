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

import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.ErrorDetail
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.Error._
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._

import scala.util.Try

trait EisHttpErrorHandler extends Logging {

  def handleErrorResponse(httpResponse: HttpResponse, correlationId: String): EisHttpErrorResponse =
    httpResponse.status match {

      case BAD_REQUEST =>
        EisHttpErrorResponse(BAD_REQUEST, determine400Error(correlationId, httpResponse.body))
      case FORBIDDEN   =>
        EisHttpErrorResponse(
          FORBIDDEN,
          ErrorResponse(
            correlationId,
            ForbiddenCode,
            ForbiddenMessage
          )
        )
      case NOT_FOUND   =>
        EisHttpErrorResponse(
          NOT_FOUND,
          ErrorResponse(
            correlationId,
            NotFoundCode,
            NotFoundMessage
          )
        )

      case METHOD_NOT_ALLOWED =>
        EisHttpErrorResponse(
          METHOD_NOT_ALLOWED,
          ErrorResponse(
            correlationId,
            MethodNotAllowedCode,
            MethodNotAllowedMessage
          )
        )

      case BAD_GATEWAY =>
        EisHttpErrorResponse(
          BAD_GATEWAY,
          ErrorResponse(
            correlationId,
            BadGatewayCode,
            BadGatewayMessage
          )
        )

      case SERVICE_UNAVAILABLE =>
        EisHttpErrorResponse(
          SERVICE_UNAVAILABLE,
          ErrorResponse(
            correlationId,
            ServiceUnavailableCode,
            ServiceUnavailableMessage
          )
        )

      case INTERNAL_SERVER_ERROR =>
        EisHttpErrorResponse(INTERNAL_SERVER_ERROR, determine500Error(correlationId, httpResponse.body))
      case _                     =>
        EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, UnexpectedErrorCode, UnexpectedErrorMessage)
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

          case "400" =>
            val errors = Try(Json.parse(message).as[ErrorDetail]).toOption
              .map(extractError(correlationId, _))
              .flatten

            ErrorResponse(
              correlationId,
              InternalErrorResponseCode,
              InternalErrorResponseMessage,
              errors
            )

          case "401" =>
            ErrorResponse(
              correlationId,
              UnauthorizedCode,
              UnauthorizedMessage
            )
          case "404" =>
            ErrorResponse(correlationId, NotFoundCode, NotFoundMessage)
          case "405" =>
            ErrorResponse(
              correlationId,
              MethodNotAllowedCode,
              MethodNotAllowedMessage
            )
          case "500" =>
            ErrorResponse(
              correlationId,
              InternalServerErrorCode,
              InternalServerErrorMessage
            )
          case "502" =>
            ErrorResponse(
              correlationId,
              BadGatewayCode,
              BadGatewayMessage
            )
          case "503" =>
            ErrorResponse(
              correlationId,
              ServiceUnavailableCode,
              ServiceUnavailableMessage
            )
          case _     =>
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
      BadRequestMessage, // Todo: would not be better to add details.errorMessage here instead of bad request as we already node that is a bad request
      extractError(correlationId, detail)
    )

  private def extractError(correlationId: String, detail: ErrorDetail) = {
    val extractErrors: Option[Seq[errors.Error]] =
      (
        for {
          o      <- detail.sourceFaultDetail
          errors <- o.detail
        } yield errors.map(detail => parseFaultDetail(detail, correlationId))
      )
        .map((s: Seq[Option[errors.Error]]) => s.flatten)
        .filter(_.nonEmpty)
    extractErrors
  }

  protected def parseFaultDetail(rawDetail: String, correlationId: String): Option[errors.Error] = {
    val regex = """error:\s*(\w+),\s*message:\s*(.*)""".r
    regex
      .findFirstMatchIn(rawDetail)
      .map(_ group 1)
      .collect {
        case InvalidOrMissingEoriCode                                 =>
          invalidRequestParameterError(
            InvalidOrMissingEori,
            InvalidOrMissingEoriCode.toInt
          )
        case EoriDoesNotExistsCode                                    =>
          invalidRequestParameterError(EoriDoesNotExists, EoriDoesNotExistsCode.toInt)
        case InvalidOrMissingActorIdCode                              =>
          invalidRequestParameterError(InvalidOrMissingActorId, InvalidOrMissingActorIdCode.toInt)
        case InvalidOrMissingTraderRefCode                            =>
          invalidRequestParameterError(InvalidOrMissingTraderRef, InvalidOrMissingTraderRefCode.toInt)
        case TraderRefIsNotUniqueCode                                 =>
          invalidRequestParameterError(TraderRefIsNotUnique, TraderRefIsNotUniqueCode.toInt)
        case InvalidOrMissingComcodeCode                              =>
          invalidRequestParameterError(InvalidOrMissingComcode, InvalidOrMissingComcodeCode.toInt)
        case InvalidOrMissingGoodsDescriptionCode                     =>
          invalidRequestParameterError(
            InvalidOrMissingGoodsDescription,
            InvalidOrMissingGoodsDescriptionCode.toInt
          )
        case InvalidOrMissingCountryOfOriginCode                      =>
          invalidRequestParameterError(
            InvalidOrMissingCountryOfOrigin,
            InvalidOrMissingCountryOfOriginCode.toInt
          )
        case InvalidOrMissingCategoryCode                             =>
          invalidRequestParameterError(InvalidOrMissingCategory, InvalidOrMissingCategoryCode.toInt)
        case InvalidOrMissingAssessmentIdCode                         =>
          invalidRequestParameterError(
            InvalidOrMissingAssessmentId,
            InvalidOrMissingAssessmentIdCode.toInt
          )
        case InvalidAssessmentPrimaryCategoryCode                     =>
          invalidRequestParameterError(
            InvalidAssessmentPrimaryCategory,
            InvalidAssessmentPrimaryCategoryCode.toInt
          )
        case InvalidAssessmentPrimaryCategoryConditionTypeCode        =>
          invalidRequestParameterError(
            InvalidAssessmentPrimaryCategoryConditionType,
            InvalidAssessmentPrimaryCategoryConditionTypeCode.toInt
          )
        case InvalidAssessmentPrimaryCategoryConditionIdCode          =>
          invalidRequestParameterError(
            InvalidAssessmentPrimaryCategoryConditionId,
            InvalidAssessmentPrimaryCategoryConditionIdCode.toInt
          )
        case InvalidAssessmentPrimaryCategoryConditionDescriptionCode =>
          invalidRequestParameterError(
            InvalidAssessmentPrimaryCategoryConditionDescription,
            InvalidAssessmentPrimaryCategoryConditionDescriptionCode.toInt
          )
        case InvalidAssessmentPrimaryCategoryConditionTraderTextCode  =>
          invalidRequestParameterError(
            InvalidAssessmentPrimaryCategoryConditionTraderText,
            InvalidAssessmentPrimaryCategoryConditionTraderTextCode.toInt
          )
        case InvalidOrMissingSupplementaryUnitCode                    =>
          invalidRequestParameterError(
            InvalidOrMissingSupplementaryUnit,
            InvalidOrMissingSupplementaryUnitCode.toInt
          )
        case InvalidOrMissingMeasurementUnitCode                      =>
          invalidRequestParameterError(
            InvalidOrMissingMeasurementUnit,
            InvalidOrMissingMeasurementUnitCode.toInt
          )
        case InvalidOrMissingComcodeEffectiveFromDateCode             =>
          invalidRequestParameterError(
            InvalidOrMissingComcodeEffectiveFromDate,
            InvalidOrMissingComcodeEffectiveFromDateCode.toInt
          )
        case InvalidOrMissingComcodeEffectiveToDateCode               =>
          invalidRequestParameterError(
            InvalidOrMissingComcodeEffectiveToDate,
            InvalidOrMissingComcodeEffectiveToDateCode.toInt
          )
        case InvalidRecordIdCode                                      =>
          invalidRequestParameterError(InvalidRecordId, InvalidRecordIdCode.toInt)
        case RecordIdDoesNotExistsCode                                =>
          invalidRequestParameterError(RecordIdDoesNotExists, RecordIdDoesNotExistsCode.toInt)
        case InvalidLastUpdatedDateCode                               =>
          invalidRequestParameterError(InvalidLastUpdatedDate, InvalidLastUpdatedDateCode.toInt)
        case InvalidPageCode                                          =>
          invalidRequestParameterError(InvalidPage, InvalidPageCode.toInt)
        case InvalidSizeCode                                          =>
          invalidRequestParameterError(InvalidSize, InvalidSizeCode.toInt)
        case AdviceRequestInProgressCode                              =>
          invalidRequestParameterError(AdviceRequestInProgressMessage, AdviceRequestInProgressCode.toInt)
        case RecordRemovedAndCanNotBeUpdatedCode                      =>
          invalidRequestParameterError(
            RecordRemovedAndCanNotBeUpdatedMessage,
            RecordRemovedAndCanNotBeUpdatedCode.toInt
          )
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
        case other                                                    =>
          logger.warn(s"[EisHttpErrorHandler] - Error code $other is not supported")
          unexpectedError("Unrecognised error number", other.toInt)
      }
  }

}
