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

import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.{ApplicationConstants, HeaderNames}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetRecordsController @Inject() (
  cc: ControllerComponents,
  routerService: RouterService,
  uuidService: UuidService
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def getTGPRecord(
    eori: String,
    recordId: String
  ): Action[AnyContent] = Action.async { implicit request =>
    request.headers.get(HeaderNames.CLIENT_ID) match {
      case Some(_) =>
        routerService
          .fetchRecord(eori, recordId)
          .fold(
            error => Status(error.status)(toJson(error.errorResponse)),
            response => Ok(toJson(response))
          )
      case None    =>
        Future.successful(
          BadRequest(
            toJson(
              ErrorResponse(
                uuidService.uuid,
                ApplicationConstants.BAD_REQUEST_CODE,
                ApplicationConstants.MISSING_HEADER_CLIENT_ID
              )
            )
          )
        )
    }
  }
}
