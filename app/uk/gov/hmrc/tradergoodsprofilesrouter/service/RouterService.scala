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
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.{BAD_REQUEST_CODE, BAD_REQUEST_MESSAGE, INTERNAL_ERROR_RESPONSE_CODE, INTERNAL_ERROR_RESPONSE_MESSAGE, INVALID_OR_EMPTY_PAYLOAD, INVALID_REQUEST_PARAMETER_CODE, NOT_FOUND_CODE, NOT_FOUND_MESSAGE, UNAUTHORIZED_CODE, UNAUTHORIZED_MESSAGE, UNEXPECTED_ERROR_CODE, UNEXPECTED_ERROR_MESSAGE}

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
  override def fetchRecord(eori: String, recordId: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, RouterError, GoodsItemRecords] =
    EitherT(
      eisConnector
        .fetchRecord(eori, recordId)
        .map(result => Right(result.goodsItemRecords.head))
        .recover {
          case UpstreamErrorResponse(message, BAD_REQUEST, _, _) =>
            Left(RouterError(BAD_REQUEST, Some(determine400Error(message))))

          case UpstreamErrorResponse(_, FORBIDDEN, _, _)                   => Left(RouterError(FORBIDDEN))
          case UpstreamErrorResponse(_, NOT_FOUND, _, _)                   => Left(RouterError(NOT_FOUND, Some(ErrorResponse(NOT_FOUND_CODE, NOT_FOUND_MESSAGE))))
          case UpstreamErrorResponse(_, METHOD_NOT_ALLOWED, _, _)          =>
            Left(RouterError(METHOD_NOT_ALLOWED))
          case UpstreamErrorResponse(message, INTERNAL_SERVER_ERROR, _, _) =>
            Left(determine500Error(message))
          case NonFatal(e)                                                 =>
            logger.error(s"Unable to send to EIS : ${e.getMessage}", e)
            Left(RouterError(INTERNAL_SERVER_ERROR))
        }
    )

  def determine400Error(message: String): ErrorResponse =
    Json.parse(message).validate[ErrorDetail] match {
      case JsSuccess(detail, _) =>
        setBadRequestResponse(detail)
      case JsError(errors)      =>
        ErrorResponse(UNEXPECTED_ERROR_CODE, UNEXPECTED_ERROR_MESSAGE)
    }

  def determine500Error(message: String): RouterError =
    Json.parse(message).validate[ErrorDetail] match {
      case JsSuccess(detail, _) =>
        detail.errorCode match {
          case "400" =>
            RouterError(
              BAD_REQUEST,
              Some(ErrorResponse(INTERNAL_ERROR_RESPONSE_CODE, INTERNAL_ERROR_RESPONSE_MESSAGE))
            )
          case "401" =>
            RouterError(
              UNAUTHORIZED,
              Some(ErrorResponse(UNAUTHORIZED_CODE, UNAUTHORIZED_MESSAGE))
            )
          case "200" =>
            RouterError(
              BAD_REQUEST,
              Some(ErrorResponse(BAD_REQUEST_CODE, INVALID_OR_EMPTY_PAYLOAD))
            )
          case _     => RouterError(INTERNAL_SERVER_ERROR)
        }
      case JsError(errors)      =>
        RouterError(INTERNAL_SERVER_ERROR)
    }

  def setBadRequestResponse(detail: ErrorDetail): ErrorResponse =
    ErrorResponse(
      BAD_REQUEST_CODE,
      BAD_REQUEST_MESSAGE,
      detail.sourceFaultDetail.map { sfd =>
        sfd.detail.map(parseFaultDetail)
      }
    )

  def parseFaultDetail(rawDetail: String): Error = {
    val regex = """error:\s*(\d+),\s*message:\s*(.*)""".r

    rawDetail match {
      case regex(code) =>
        code match {
          case "006" => Error(INVALID_REQUEST_PARAMETER_CODE, "006 - Mandatory field comcode was missing from body")
          case "007" => Error(INVALID_REQUEST_PARAMETER_CODE, "007 - eori doesn’t exist in the database")
          case "025" => Error(INVALID_REQUEST_PARAMETER_CODE, "025 - Invalid optional request parameter")
          case "026" => Error(INVALID_REQUEST_PARAMETER_CODE, "026 - recordId doesn’t exist in the database")
          case "028" => Error(INVALID_REQUEST_PARAMETER_CODE, "028 - Invalid optional request parameter")
          case "029" => Error(INVALID_REQUEST_PARAMETER_CODE, "029 - Invalid optional request parameter")
          case "030" => Error(INVALID_REQUEST_PARAMETER_CODE, "030 - Invalid optional request parameter")
          case _     => Error(UNEXPECTED_ERROR_CODE, UNEXPECTED_ERROR_MESSAGE)
        }

      case _ =>
        throw new IllegalArgumentException(s"Unable to parse fault detail: $rawDetail")
    }
  }
}
