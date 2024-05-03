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
        .map(result => Right(result.goodsItemRecords.headOption))
        .recover {
          case UpstreamErrorResponse(message, BAD_REQUEST, _, _) =>
            Left(RouterError(BAD_REQUEST, Some(determine400Error(message))))

          case UpstreamErrorResponse(_, FORBIDDEN, _, _)                   => Left(RouterError(FORBIDDEN))
          case UpstreamErrorResponse(_, NOT_FOUND, _, _)                   => Left(RouterError(NOT_FOUND))
          case UpstreamErrorResponse(_, METHOD_NOT_ALLOWED, _, _)          =>
            Left(RouterError(METHOD_NOT_ALLOWED))
          case UpstreamErrorResponse(message, INTERNAL_SERVER_ERROR, _, _) =>
            Left(determine500Error(eori, Some(recordId), message))
          case NonFatal(e)                                                 =>
            logger.error(s"Unable to send to EIS : ${e.getMessage}", e)
            Left(RouterError(INTERNAL_SERVER_ERROR))
        }
    )

  def determine400Error(message: String): ErrorResponse =
    Json.parse(message).validate[ErrorDetail] match {
      case JsSuccess(detail, _) =>
        detail.errorCode match {
          case "400" => setBadRequestResponse(detail)
          case _     => ErrorResponse("UNEXPECTED_ERROR", "Unexpected error")
        }
      case JsError(errors)      =>
        ErrorResponse("UNEXPECTED_ERROR", "Unexpected error")
    }

  def determine500Error(eori: String, recordId: Option[String] = None, message: String): RouterError =
    Json.parse(message).validate[ErrorDetail] match {
      case JsSuccess(detail, _) =>
        detail.errorCode match {
          case "400" =>
            RouterError(
              BAD_REQUEST,
              Some(ErrorResponse("INTERNAL_ERROR_RESPONSE", "Internal Error response"))
            )
          case "401" =>
            RouterError(
              UNAUTHORIZED,
              Some(ErrorResponse("UNAUTHORIZED", "Unauthorised"))
            )
          case "200" =>
            RouterError(
              BAD_REQUEST,
              Some(ErrorResponse("BAD_REQUEST", "Invalid Response Payload or Empty payload"))
            )
          case _     => RouterError(INTERNAL_SERVER_ERROR)
        }
      case JsError(errors)      =>
        RouterError(INTERNAL_SERVER_ERROR)
    }

  def setBadRequestResponse(detail: ErrorDetail): ErrorResponse =
    ErrorResponse(
      "BAD_REQUEST",
      "Bad request",
      detail.sourceFaultDetail.map { sfd =>
        sfd.detail.map(parseFaultDetail)
      }
    )

  def parseFaultDetail(rawDetail: String): Error = {
    val regex = """error:\s*(\d+),\s*message:\s*(.*)""".r

    rawDetail match {
      case regex(code) =>
        code match {
          case "006" => Error("INVALID_REQUEST_PARAMETER", "006 - Mandatory field comcode was missing from body")
          case "007" => Error("INVALID_REQUEST_PARAMETER", "007 - eori doesn’t exist in the database")
          case "025" => Error("INVALID_REQUEST_PARAMETER", "025 - Invalid optional request parameter")
          case "026" => Error("INVALID_REQUEST_PARAMETER", "026 - recordId doesn’t exist in the database")
          case "028" => Error("INVALID_REQUEST_PARAMETER", "028 - Invalid optional request parameter")
          case "029" => Error("INVALID_REQUEST_PARAMETER", "029 - Invalid optional request parameter")
          case "030" => Error("INVALID_REQUEST_PARAMETER", "030 - Invalid optional request parameter")
          case _     => Error("UNKNOWN_ERROR", "Unknown error")
        }

      case _ =>
        throw new IllegalArgumentException(s"Unable to parse fault detail: $rawDetail")
    }
  }
}
