/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.http.Status.{INTERNAL_SERVER_ERROR, isSuccessful}
import play.api.libs.Files.logger
import play.api.libs.json.{JsResult, Json, Reads}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.RequestBuilder
import uk.gov.hmrc.http.{HttpErrorFunctions, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success, Try}

trait BaseConnector extends HttpErrorFunctions {

  implicit class HttpResponseHelpers(response: HttpResponse) {

    def as[A](implicit reads: Reads[A], tt: TypeTag[A]): Future[A] =
      Try(Json.parse(response.body)) match {
        case Success(value)     =>
          value
            .validate[A]
            .map(result => Future.successful(result))
            .recoverTotal { error =>
              logger.error(
                s"[EisConnector] - Failed to validate or parse JSON body of type: ${typeOf[A]}",
                error
              )
              Future.failed(JsResult.Exception(error))
            }
        case Failure(exception) =>
          logger.error(
            s"[EisConnector] - Response body could not be parsed as JSON, body: ${response.body}",
            exception
          )
          Future.failed(
            UpstreamErrorResponse(s"Response body could not be read: ${response.body}", INTERNAL_SERVER_ERROR)
          )
      }

    def error[A]: Future[A] =
      Future.failed(UpstreamErrorResponse(response.body, response.status))

  }

  implicit class RequestBuilderHelpers(requestBuilder: RequestBuilder) {
    def executeAndDeserialise[T](implicit ec: ExecutionContext, reads: Reads[T], tt: TypeTag[T]): Future[T] =
      requestBuilder
        .execute[HttpResponse]
        .flatMap {
          case response if isSuccessful(response.status) => response.as[T]
          case response                                  => response.error
        }

    def executeAndExpect(expected: Int)(implicit ec: ExecutionContext): Future[Int] =
      requestBuilder
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case `expected` => Future.successful(expected)
            case _          => response.error
          }
        }
  }

}
