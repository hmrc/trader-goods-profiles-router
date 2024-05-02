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
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, METHOD_NOT_ALLOWED, NOT_FOUND}
import play.api.libs.Files.logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{ErrorDetail, GetEisRecordsResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{EisError, InvalidEisError}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[RouterServiceImpl])
trait RouterService {
  def fetchRecord(
    eori: String,
    recordId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): EitherT[Future, EisError, GetEisRecordsResponse]
}

class RouterServiceImpl @Inject() (eisConnector: EISConnector) extends RouterService {
  override def fetchRecord(eori: String, recordId: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, EisError, GetEisRecordsResponse] =
    EitherT(
      eisConnector
        .fetchRecord(eori, recordId)
        .map(result => Right(result))
        .recover {
          case UpstreamErrorResponse(message, BAD_REQUEST, _, _)           => Left(determineFetchRecordError(message))
          case UpstreamErrorResponse(_, FORBIDDEN, _, _)                   => Left(EisError.Forbidden(eori))
          case UpstreamErrorResponse(_, NOT_FOUND, _, _)                   => Left(EisError.NotFound(eori))
          case UpstreamErrorResponse(_, METHOD_NOT_ALLOWED, _, _)          => Left(EisError.MethodNotAllowed(eori))
          case UpstreamErrorResponse(message, INTERNAL_SERVER_ERROR, _, _) => Left(determineFetchRecordError(message))
          case NonFatal(e)                                                 =>
            logger.error(s"Unable to send to EIS : ${e.getMessage}", e)
            Left(EisError.UnexpectedError(Some(e)))
        }
    )

  def determineFetchRecordError(message: String): EisError =
    Json.parse(message).validate[ErrorDetail] match {
      case JsSuccess(detail, _) =>
        detail.errorCode match {
          case "400" => null // setBadRequestResponse(detail)
          case _     => EisError.UnexpectedError(Some(new RuntimeException(detail.errorMessage)))
        }
      case JsError(errors)      =>
        EisError.UnexpectedError(Some(new RuntimeException(s"Error parsing response: $errors")))
    }
}
