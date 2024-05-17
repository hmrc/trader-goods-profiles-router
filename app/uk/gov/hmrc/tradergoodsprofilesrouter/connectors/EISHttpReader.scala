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
import play.api.mvc.Results.{BadRequest, Forbidden, InternalServerError, MethodNotAllowed, NotFound}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{ErrorDetail, GetEisRecordsResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants

import scala.concurrent.Future
import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success, Try}

object EISHttpReader {
  def responseHandler[T](
    httpResponse: HttpResponse
  )(implicit correlationId: String): Future[Either[Result, GetEisRecordsResponse]] =
    httpResponse match {
      case response if isSuccessful(response.status) =>
        Future.successful(Right(jsonAs[GetEisRecordsResponse](response)))
      case response                                  => Future.successful(Left(handleErrorResponse(response)))
    }

  private def handleErrorResponse(httpResponse: HttpResponse)(implicit correlationId: String): Result =
    httpResponse.status match {
      case BAD_REQUEST => BadRequest(Json.toJson(determine400Error(correlationId, httpResponse.body)))
      case FORBIDDEN   =>
        Forbidden(
          Json.toJson(
            ErrorResponse(
              correlationId,
              ApplicationConstants.ForbiddenCode,
              ApplicationConstants.ForbiddenMessage
            )
          )
        )
      case NOT_FOUND   =>
        NotFound(
          Json.toJson(
            ErrorResponse(
              correlationId,
              ApplicationConstants.NotFoundCode,
              ApplicationConstants.NotFoundMessage
            )
          )
        )

      case METHOD_NOT_ALLOWED    =>
        MethodNotAllowed(
          Json.toJson(
            ErrorResponse(
              correlationId,
              ApplicationConstants.MethodNotAllowedCode,
              ApplicationConstants.MethodNotAllowedMessage
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
              ApplicationConstants.UnexpectedErrorCode,
              ApplicationConstants.UnexpectedErrorMessage
            )
          )
        )
    }

  def jsonAs[A](response: HttpResponse)(implicit reads: Reads[A], tt: TypeTag[A]): A =
    Try(Json.parse(response.body)) match {
      case Success(value)     =>
        value
          .validate[A]
          .map(result => result)
          .recoverTotal { error: JsError =>
            logger.error(
              s"[EisConnector] - Failed to validate or parse JSON body of type: ${typeOf[A]}",
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
          case "200" =>
            ErrorResponse(
              correlationId,
              ApplicationConstants.InvalidOrEmptyPayloadCode,
              ApplicationConstants.InvalidOrEmptyPayloadMessage
            )
          case "400" =>
            ErrorResponse(
              correlationId,
              ApplicationConstants.InternalErrorResponseCode,
              ApplicationConstants.InternalErrorResponseMessage
            )
          case "401" =>
            ErrorResponse(
              correlationId,
              ApplicationConstants.UnauthorizedCode,
              ApplicationConstants.UnauthorizedMessage
            )
          case "404" =>
            ErrorResponse(correlationId, ApplicationConstants.NotFoundCode, ApplicationConstants.NotFoundMessage)
          case "405" =>
            ErrorResponse(
              correlationId,
              ApplicationConstants.MethodNotAllowedCode,
              ApplicationConstants.MethodNotAllowedMessage
            )
          case "500" =>
            ErrorResponse(
              correlationId,
              ApplicationConstants.InternalServerErrorCode,
              ApplicationConstants.InternalServerErrorMessage
            )
          case "502" =>
            ErrorResponse(
              correlationId,
              ApplicationConstants.BadGatewayCode,
              ApplicationConstants.BadGatewayMessage
            )
          case "503" =>
            ErrorResponse(
              correlationId,
              ApplicationConstants.ServiceUnavailableCode,
              ApplicationConstants.ServiceUnavailableMessage
            )
          case _     =>
            ErrorResponse(correlationId, ApplicationConstants.UnknownCode, ApplicationConstants.UnknownMessage)
        }
      case JsError(_)           =>
        ErrorResponse(
          correlationId,
          ApplicationConstants.UnexpectedErrorCode,
          ApplicationConstants.UnexpectedErrorMessage
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
