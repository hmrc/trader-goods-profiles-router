/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.http.Status.*
import play.api.libs.json.*
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.reflect.ClassTag

object EisHttpReader extends Logging {

  case class HttpReader[T](correlationId: String, errorHandler: (HttpResponse, String) => EisHttpErrorResponse)(implicit
                                                                                                                reads: Reads[T],
                                                                                                                ct: ClassTag[T]
  ) extends HttpReads[Either[EisHttpErrorResponse, T]] {
    override def read(method: String, url: String, response: HttpResponse): Either[EisHttpErrorResponse, T] =
      response match {
        case response if isSuccessful(response.status) =>
          Right(parseJson[T](response))
        case response =>
          logger.warn(
            s"[HttpReader] - Downstream error, method: $method, url: $url, correlationId: $correlationId, body: ${response.body}"
          )
          Left(errorHandler(response, correlationId))
      }
  }

  case class StatusHttpReader(correlationId: String, errorHandler: (HttpResponse, String) => EisHttpErrorResponse)
    extends HttpReads[Either[EisHttpErrorResponse, Int]] {
    override def read(method: String, url: String, response: HttpResponse): Either[EisHttpErrorResponse, Int] =
      response match {
        case response if isSuccessful(response.status) => Right(response.status)
        case response =>
          logger.warn(
            s"[StatusHttpReader] - Downstream error, method: $method, url: $url, correlationId: $correlationId, body: ${response.body}"
          )
          Left(errorHandler(response, correlationId))
      }
  }

  def parseJson[T](response: HttpResponse)(implicit reads: Reads[T], ct: ClassTag[T]): T = {
    response.json.validate[T] match {
      case JsSuccess(result, _) => result
      case JsError(errors) =>
        val errorMsg = errors.map { case (path, validationErrors) =>
          s"$path -> ${validationErrors.map(_.message).mkString(", ")}"
        }.mkString("; ")

        logger.warn(
          s"[EisHttpReader] - JSON validation failed for type: ${ct.runtimeClass.getSimpleName}, errors: $errorMsg"
        )
        throw new RuntimeException(s"Response body could not be parsed as type ${ct.runtimeClass.getSimpleName}")
    }
  }

  def isSuccessful(status: Int): Boolean = status >= 200 && status < 300
}
