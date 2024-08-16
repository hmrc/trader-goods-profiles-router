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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.{AuthAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{GetProfileService, UuidService}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetProfileController @Inject() (
  authAction: AuthAction,
  override val controllerComponents: ControllerComponents,
  getProfileService: GetProfileService,
  override val uuidService: UuidService
)(implicit val ec: ExecutionContext)
    extends BackendBaseController
    with ValidationRules {

  def getProfile(eori: String): Action[AnyContent] = authAction(eori).async { implicit request: Request[AnyContent] =>
    (for {
      _        <- EitherT.fromEither[Future](validateAcceptHeader)
      response <-
        EitherT(getProfileService.getProfile(eori)).leftMap(e => Status(e.httpStatus)(Json.toJson(e.errorResponse)))
    } yield (Ok(Json.toJson(response)))).merge
  }
}
