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
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.fieldsToErrorCode
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.{AuthAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.MaintainProfileRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{MaintainProfileService, UuidService}

import scala.concurrent.{ExecutionContext, Future}

class MaintainProfileController @Inject() (
  authAction: AuthAction,
  override val controllerComponents: ControllerComponents,
  maintainProfileService: MaintainProfileService,
  override val uuidService: UuidService
)(implicit val ec: ExecutionContext)
    extends BackendBaseController
    with ValidationRules
    with Logging {

  def maintain(eori: String): Action[JsValue] = authAction(eori).async(parse.json) { implicit request =>
    val result = for {
      _                      <- EitherT.fromEither[Future](validateAcceptHeader)
      maintainProfileRequest <-
        EitherT.fromEither[Future](validateRequestBody[MaintainProfileRequest](fieldsToErrorCode))
      response               <- EitherT(maintainProfileService.maintainProfile(eori, maintainProfileRequest))
                                  .leftMap(e => Status(e.httpStatus)(Json.toJson(e.errorResponse)))
    } yield Ok(Json.toJson(response))

    result.merge
  }
}
