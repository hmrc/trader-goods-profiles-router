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
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success, Try}

object EisHttpReader {

  case class HttpReader[T](correlationId: String, errorHandler: (HttpResponse, String) => EisHttpErrorResponse)(implicit
    reads: Reads[T],
    tt: TypeTag[T]
  ) extends HttpReads[Either[EisHttpErrorResponse, T]] {
    override def read(method: String, url: String, response: HttpResponse): Either[EisHttpErrorResponse, T] =
      response match {
        case response if isSuccessful(response.status) =>
          Right(jsonAs[T](response))
        case response                                  =>
          logger.warn(
            s"[HttpReader] - downstream error, method: $method, url: $url, correlationId: $correlationId, body: ${response.body} "
          )
          Left(errorHandler(response, correlationId))
      }
  }

  case class StatusHttpReader(correlationId: String, errorHandler: (HttpResponse, String) => EisHttpErrorResponse)
      extends HttpReads[Either[EisHttpErrorResponse, Int]] {
    override def read(method: String, url: String, response: HttpResponse): Either[EisHttpErrorResponse, Int] =
      response match {
        case response if isSuccessful(response.status) => Right(response.status)
        case response                                  =>
          logger.warn(
            s"[StatusHttpReader] - downstream error, method: $method, url: $url, correlationId: $correlationId, body: ${response.body} "
          )
          Left(errorHandler(response, correlationId))
      }
  }

  def jsonAs[T](response: HttpResponse)(implicit reads: Reads[T], tt: TypeTag[T]): T =
    Try(Json.parse(response.body)) match {
      case Success(value)     =>
        value
          .validate[T]
          .map(result => result)
          .recoverTotal { error: JsError =>
            logger.warn(
              s"[EisHttpReader] - Failed to validate or parse JSON body of type: ${typeOf[T]}",
              error
            )
            throw new RuntimeException(s"Response body could not be read as type ${typeOf[T]}")
          }
      case Failure(exception) =>
        logger.warn(
          s"[EisHttpReader] - Response body could not be parsed as JSON, body: ${response.body}",
          exception
        )
        throw new RuntimeException(s"Response body could not be read: ${response.body}")
    }
}
