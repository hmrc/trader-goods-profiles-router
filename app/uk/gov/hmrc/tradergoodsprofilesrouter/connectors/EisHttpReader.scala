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
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success, Try}

object EisHttpReader {

  case class HttpReader[T](correlationId: String, errorHandler: (HttpResponse, String) => Result)(implicit
    reads: Reads[T],
    tt: TypeTag[T]
  ) extends HttpReads[Either[Result, T]] {
    override def read(method: String, url: String, response: HttpResponse): Either[Result, T] =
      response match {
        case response if isSuccessful(response.status) =>
          Right(jsonAs[T](response))
        case response                                  => Left(errorHandler(response, correlationId))
      }
  }

  case class RemoveRecordHttpReader[T](correlationId: String, errorHandler: (HttpResponse, String) => Result)
      extends HttpReads[Either[Result, Int]] {
    override def read(method: String, url: String, response: HttpResponse): Either[Result, Int] =
      response match {
        case response if isSuccessful(response.status) => Right(response.status)
        case response                                  => Left(errorHandler(response, correlationId))
      }
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
            throw new RuntimeException(s"Response body could not be read as type ${typeOf[T]}")
          }
      case Failure(exception) =>
        logger.error(
          s"[EisConnector] - Response body could not be parsed as JSON, body: ${response.body}",
          exception
        )
        throw new RuntimeException(s"Response body could not be read: ${response.body}")
    }
}
