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
import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.{AuthAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.fieldsToErrorCode
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateProfileRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.CreateProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{CreateProfileService, UuidService}

import scala.concurrent.{ExecutionContext, Future}

class CreateProfileController @Inject() (
  authAction: AuthAction,
  override val controllerComponents: ControllerComponents,
  createProfileService: CreateProfileService,
  override val uuidService: UuidService
)(implicit val ec: ExecutionContext)
    extends BackendBaseController
    with ValidationRules
    with Logging {

  def create(eori: String): Action[JsValue] = authAction(eori).async(parse.json) { implicit request =>
    val result = for {
      _                    <- EitherT.fromEither[Future](validateAcceptHeader)
      createProfileRequest <-
        EitherT.fromEither[Future](validateRequestBody[CreateProfileRequest](fieldsToErrorCode))
      response             <- createProfile(eori, createProfileRequest)
    } yield Ok(Json.toJson(response))

    result.merge
  }

  private def createProfile(
    eori: String,
    createProfileRequest: CreateProfileRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, CreateProfileResponse] =
    EitherT(
      createProfileService.createProfile(eori, createProfileRequest)
    )
      .leftMap(e => Status(e.httpStatus)(Json.toJson(e.errorResponse)))
}
