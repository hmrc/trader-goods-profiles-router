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

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[RouterServiceImpl])
trait RouterService {
  def fetchRecord(
    eori: String,
    recordId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): EitherT[Future, RouterError, GoodsItemRecords]
}

class RouterServiceImpl @Inject() (eisConnector: EISConnector) extends RouterService {
  val correlationId                                 = UUID.randomUUID().toString
  override def fetchRecord(eori: String, recordId: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, RouterError, GoodsItemRecords] =
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
                  ApplicationConstants.FORBIDDEN_CODE,
                  ApplicationConstants.FORBIDDEN_MESSAGE
                )
              )
            )
          case UpstreamErrorResponse(_, NOT_FOUND, _, _)                   =>
            Left(
              RouterError(
                NOT_FOUND,
                ErrorResponse(
                  correlationId,
                  ApplicationConstants.NOT_FOUND_CODE,
                  ApplicationConstants.NOT_FOUND_MESSAGE
                )
              )
            )
          case UpstreamErrorResponse(_, METHOD_NOT_ALLOWED, _, _)          =>
            Left(
              RouterError(
                METHOD_NOT_ALLOWED,
                ErrorResponse(
                  correlationId,
                  ApplicationConstants.METHOD_NOT_ALLOWED_CODE,
                  ApplicationConstants.METHOD_NOT_ALLOWED_MESSAGE
                )
              )
            )
          case UpstreamErrorResponse(message, INTERNAL_SERVER_ERROR, _, _) =>
            Left(determine500Error(correlationId, message))
          case NonFatal(e)                                                 =>
            logger.error(s"Unable to send to EIS : ${e.getMessage}", e)
            Left(
              RouterError(
                INTERNAL_SERVER_ERROR,
                ErrorResponse(
                  correlationId,
                  ApplicationConstants.UNEXPECTED_ERROR_CODE,
                  ApplicationConstants.UNEXPECTED_ERROR_MESSAGE
                )
              )
            )
        }
    )

  def determine400Error(correlationId: String, message: String): ErrorResponse =
    Json.parse(message).validate[ErrorDetail] match {
      case JsSuccess(detail, _) =>
        setBadRequestResponse(correlationId, detail)
      case JsError(errors)      =>
        ErrorResponse(
          correlationId,
          ApplicationConstants.UNEXPECTED_ERROR_CODE,
          ApplicationConstants.UNEXPECTED_ERROR_MESSAGE
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
                ApplicationConstants.INVALID_OR_EMPTY_PAYLOAD_CODE,
                ApplicationConstants.INVALID_OR_EMPTY_PAYLOAD_MESSAGE
              )
            )
          case "400" =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(
                correlationId,
                ApplicationConstants.INTERNAL_ERROR_RESPONSE_CODE,
                ApplicationConstants.INTERNAL_ERROR_RESPONSE_MESSAGE
              )
            )
          case "401" =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(
                correlationId,
                ApplicationConstants.UNAUTHORIZED_CODE,
                ApplicationConstants.UNAUTHORIZED_MESSAGE
              )
            )
          case "404" =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(correlationId, ApplicationConstants.NOT_FOUND_CODE, ApplicationConstants.NOT_FOUND_MESSAGE)
            )
          case "405" =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(
                correlationId,
                ApplicationConstants.METHOD_NOT_ALLOWED_CODE,
                ApplicationConstants.METHOD_NOT_ALLOWED_MESSAGE
              )
            )
          case "502" =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(
                correlationId,
                ApplicationConstants.BAD_GATEWAY_CODE,
                ApplicationConstants.BAD_GATEWAY_MESSAGE
              )
            )
          case "503" =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(
                correlationId,
                ApplicationConstants.SERVICE_UNAVAILABLE_CODE,
                ApplicationConstants.SERVICE_UNAVAILABLE_MESSAGE
              )
            )
          case _     =>
            RouterError(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(correlationId, ApplicationConstants.UNKNOWN_CODE, ApplicationConstants.UNKNOWN_MESSAGE)
            )
        }
      case JsError(errors)      =>
        RouterError(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(
            correlationId,
            ApplicationConstants.UNEXPECTED_ERROR_CODE,
            ApplicationConstants.UNEXPECTED_ERROR_MESSAGE
          )
        )
    }

  def setBadRequestResponse(correlationId: String, detail: ErrorDetail): ErrorResponse =
    ErrorResponse(
      correlationId,
      ApplicationConstants.BAD_REQUEST_CODE,
      ApplicationConstants.BAD_REQUEST_MESSAGE,
      detail.sourceFaultDetail.map { sfd =>
        sfd.detail.map(parseFaultDetail)
      }
    )

  def parseFaultDetail(rawDetail: String): Error = {
    val regex = """error:\s*(\d+),\s*message:\s*(.*)""".r

    rawDetail match {
      case regex(code) =>
        code match {
          case "006" =>
            Error(
              ApplicationConstants.INVALID_REQUEST_PARAMETER_CODE,
              ApplicationConstants.INVALID_OR_MISSING_EORI
            )
          case "007" =>
            Error(ApplicationConstants.INVALID_REQUEST_PARAMETER_CODE, ApplicationConstants.EORI_DOES_NOT_EXISTS)
          case "025" =>
            Error(ApplicationConstants.INVALID_REQUEST_PARAMETER_CODE, ApplicationConstants.INVALID_RECORD_ID)
          case "026" =>
            Error(ApplicationConstants.INVALID_REQUEST_PARAMETER_CODE, ApplicationConstants.RECORD_ID_DOES_NOT_EXISTS)
          case _     => Error(ApplicationConstants.UNEXPECTED_ERROR_CODE, ApplicationConstants.UNEXPECTED_ERROR_MESSAGE)
        }

      case _ =>
        throw new IllegalArgumentException(s"Unable to parse fault detail: $rawDetail")
    }
  }
}
