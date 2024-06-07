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
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.UpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{UpdateRecordService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.{BadRequestCode, BadRequestMessage}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ValidationSupport.optionalFieldsToErrorCode

import scala.concurrent.{ExecutionContext, Future}

class UpdateRecordController @Inject() (
  override val controllerComponents: ControllerComponents,
  updateRecordService: UpdateRecordService,
  override val uuidService: UuidService
)(implicit override val ec: ExecutionContext)
    extends BackendBaseController
    with ValidationRules
    with Logging {

  def update(eori: String, recordId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val result = for {
      _                   <- EitherT.fromEither[Future](validateClientId).leftMap(e => badRequest(Seq(e)))
      _                   <- EitherT.fromEither[Future](validateRecordId(recordId)).leftMap(e => badRequest(Seq(e)))
      updateRecordRequest <- validateRequestBody
      response            <- updateRecordService.updateRecord(eori, recordId, updateRecordRequest)
    } yield Ok(Json.toJson(response))

    result.merge
  }

  def badRequest(errors: Seq[Error])                                                                                =
    BadRequest(
      toJson(
        ErrorResponse(
          uuidService.uuid,
          BadRequestCode,
          BadRequestMessage,
          Some(errors)
        )
      )
    )
  private def validateRequestBody(implicit request: Request[JsValue]): EitherT[Future, Result, UpdateRecordRequest] =
    EitherT
      .fromEither[Future](validateRequestBody[UpdateRecordRequest](optionalFieldsToErrorCode))
      .leftMap(e => badRequest(e))

}
