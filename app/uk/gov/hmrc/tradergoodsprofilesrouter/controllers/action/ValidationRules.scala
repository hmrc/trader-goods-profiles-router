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

package uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action

import cats.data.EitherT
import cats.implicits.catsSyntaxTuple2Parallel
import cats.syntax.all._
import play.api.libs.json.{JsValue, Reads}
import play.api.mvc.{BaseController, Request, Result}
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.ValidatedQueryParameters
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{BadRequestErrorResponse, Error}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.{HeaderNames, ValidationSupport}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

//todo: we may want to unify ValidationSupport and this one.
trait ValidationRules {
  this: BaseController =>

  def uuidService: UuidService

  implicit def ec: ExecutionContext

  protected def validateClientId(implicit request: Request[_]): EitherT[Future, Result, String] =
    EitherT
      .fromOption[Future](
        request.headers.get(HeaderNames.ClientId),
        Error(InvalidHeader, MissingHeaderClientId, 6000)
      )
      .leftMap(e => BadRequestErrorResponse(uuidService.uuid, Seq(e)).asPresentation)

  protected def validateRecordId(recordId: String): Either[Error, String] =
    Try(UUID.fromString(recordId).toString).toOption.toRight(
      Error(
        InvalidQueryParameter,
        InvalidRecordIdQueryParameter,
        InvalidRecordIdCode.toInt
      )
    )

  protected def validateActorId(actorId: String): Either[Error, String] = {
    val pattern = "^[A-Z]{2}\\d{12,15}$".r
    pattern
      .findFirstIn(actorId)
      .toRight(
        Error(
          InvalidQueryParameter,
          InvalidActorIdQueryParameter,
          InvalidOrMissingActorIdCode.toInt
        )
      )
  }

  protected def validateRequestBody[A: Reads](
    fieldToErrorCodeTable: Map[String, (String, String)]
  )(implicit request: Request[JsValue]): EitherT[Future, Result, A] =
    EitherT
      .fromEither[Future](
        request.body
          .validate[A]
          .asEither
      )
      .leftMap { errors =>
        BadRequestErrorResponse(
          uuidService.uuid,
          ValidationSupport.convertError[A](errors, fieldToErrorCodeTable)
        ).asPresentation
      }

  def validateQueryParameters(actorId: String, recordId: String) =
    (
      validateActorId(actorId).toEitherNec,
      validateRecordId(recordId).toEitherNec
    ).parMapN { (validatedActorId, validatedRecordId) =>
      ValidatedQueryParameters(validatedActorId, validatedRecordId)
    } leftMap { errors =>
      errors.toList
    }
}

object ValidationRules {

  final case class ValidatedQueryParameters(actorId: String, recordId: String)
}
