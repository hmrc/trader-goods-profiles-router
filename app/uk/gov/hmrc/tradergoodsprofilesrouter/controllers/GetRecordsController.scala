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
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc._
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
  ): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val result = for {
      _          <- validateClientId
      recordItem <- routerService.fetchRecord(eori, recordId)
    } yield Ok(Json.toJson(recordItem))

    result.merge
  }

  private def validateClientId(implicit request: Request[AnyContent]): EitherT[Future, Result, String] =
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
