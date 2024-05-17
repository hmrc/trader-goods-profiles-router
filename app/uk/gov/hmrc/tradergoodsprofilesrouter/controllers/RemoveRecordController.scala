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

package uk.gov.hmrc.tradergoodsprofilesrouter.controllers

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.RemoveRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.{ApplicationConstants, HeaderNames}

import scala.concurrent.{ExecutionContext, Future}

class RemoveRecordController @Inject() (
  cc: ControllerComponents,
  routerService: RouterService,
  uuidService: UuidService
)(implicit
  executionContext: ExecutionContext
) extends BackendController(cc)
    with Logging {

  def remove(eori: String, recordId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val result = for {
      _                   <- validateClientId
      removeRecordRequest <- validateRemoveRecordRequest(request)
      _                   <- routerService.removeRecord(eori, recordId, removeRecordRequest.actorId)
    } yield Ok

    result.merge
  }

  private def validateRemoveRecordRequest(request: Request[JsValue]): EitherT[Future, Result, RemoveRecordRequest] =
    request.body
      .validate[RemoveRecordRequest]
      .asEither
      .leftMap { _ =>
        logger.warn(
          "[RemoveRecordController] - Remove Record Validation JsError in RemoveRecordController.remove"
        )
        BadRequest(
          toJson(
            ErrorResponse(
              uuidService.uuid,
              ApplicationConstants.BadRequestCode,
              ApplicationConstants.BadRequestMessage,
              Some(
                Seq(
                  Error(
                    ApplicationConstants.InvalidOrMissingActorIdCode,
                    ApplicationConstants.InvalidOrMissingActorId
                  )
                )
              )
            )
          )
        ): Result
      }
      .toEitherT[Future]

  private def validateClientId(implicit request: Request[JsValue]): EitherT[Future, Result, String] =
    EitherT.fromOption(
      request.headers.get(HeaderNames.ClientId),
      BadRequest(
        toJson(
          ErrorResponse(
            uuidService.uuid,
            ApplicationConstants.BadRequestCode,
            ApplicationConstants.MissingHeaderClientId
          )
        )
      )
    )
}
