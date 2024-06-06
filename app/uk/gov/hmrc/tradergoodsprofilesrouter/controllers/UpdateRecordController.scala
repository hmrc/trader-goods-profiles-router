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
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidateHeaderClientId
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.UpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.{BadRequestCode, BadRequestMessage}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ValidationSupport

import scala.concurrent.{ExecutionContext, Future}

class UpdateRecordController @Inject() (
  cc: ControllerComponents,
  routerService: RouterService,
  uuidService: UuidService,
  validateHeaderClientId: ValidateHeaderClientId
)(implicit
  executionContext: ExecutionContext
) extends BackendController(cc)
    with Logging {

  def update(eori: String, recordId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    //todo: validate eori and recordId
    val result = for {
      _ <- validateHeaderClientId.validateClientId(request)

      updateRecordRequest <- validateRequestBody(request)

      response <- routerService.updateRecord(eori, recordId, updateRecordRequest)
    } yield Ok(Json.toJson(response))

    result.merge
  }

  private def validateRequestBody(implicit request: Request[JsValue]): EitherT[Future, Result, UpdateRecordRequest] =
    request.body
      .validate[UpdateRecordRequest]
      .asEither
      .leftMap { errors =>
        logger.warn(
          "[UpdateRecordController] - Update Record Validation JsError in UpdateRecordController.create"
        )
        BadRequest(
          toJson(
            ErrorResponse(
              uuidService.uuid,
              BadRequestCode,
              BadRequestMessage,
              Some(ValidationSupport.convertError[UpdateRecordRequest](errors))
            )
          )
        ): Result
      }
      .toEitherT[Future]

}
