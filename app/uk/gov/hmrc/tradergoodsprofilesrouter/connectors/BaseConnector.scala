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

import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.Files.logger
import play.api.libs.json.{JsResult, Json, Reads}
import play.api.mvc.Result
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.RequestBuilder
import uk.gov.hmrc.http.{HttpErrorFunctions, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISHttpReader.responseHandler
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
//import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISHttpReader

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

  implicit class RequestBuilderHelpers(requestBuilder: RequestBuilder)(implicit correlationId: String) {
    def executeAndDeserialise[T](implicit
      ec: ExecutionContext,
      reads: Reads[T],
      tt: TypeTag[T]
    ): Future[Either[Result, GetEisRecordsResponse]] =
      requestBuilder
        .execute[HttpResponse]
        .flatMap(responseHandler)

    /**
      * This method will be used for other endpoint e.g. delete
      */
    def executeAndExpect(expected: Int)(implicit ec: ExecutionContext): Future[Unit] =
      requestBuilder
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case `expected` => Future.successful(())
            case _          => response.error
          }
        }
  }

}
