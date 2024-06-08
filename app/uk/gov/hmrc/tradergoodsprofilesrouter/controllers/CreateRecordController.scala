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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.BadRequestErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{CreateRecordService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ValidationSupport.fieldsToErrorCode

import scala.concurrent.{ExecutionContext, Future}

class CreateRecordController @Inject() (
  override val controllerComponents: ControllerComponents,
  createRecordService: CreateRecordService,
  override val uuidService: UuidService
)(implicit val ec: ExecutionContext)
    extends BackendBaseController
    with ValidationRules
    with Logging {

  def create(eori: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val result = for {
      _                   <- EitherT
                               .fromEither[Future](validateClientId)
                               .leftMap(e => BadRequestErrorResponse(uuidService.uuid, Seq(e)).asPresentation)
      createRecordRequest <- validateRequest(request)
      response            <- createRecordService.createRecord(eori, createRecordRequest)
    } yield Created(Json.toJson(response))

    result.merge
  }

  private def validateRequest(implicit request: Request[JsValue]): EitherT[Future, Result, CreateRecordRequest] =
    EitherT
      .fromEither[Future](validateRequestBody[CreateRecordRequest](fieldsToErrorCode))
      .leftMap(e => BadRequestErrorResponse(uuidService.uuid, e).asPresentation)

}
