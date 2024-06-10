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
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.mvc.Results.BadRequest
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.{BadRequestCode, MissingHeaderClientId}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.{ApplicationConstants, HeaderNames}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidateHeaderClientId @Inject() (uuidService: UuidService)(implicit ec: ExecutionContext) {

  def validateClientId(request: Request[JsValue]): EitherT[Future, Result, String] =
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

  def validateClientIdFromAnyContent(request: Request[AnyContent]): EitherT[Future, Result, String] =
    EitherT.fromOption(
      request.headers.get(HeaderNames.ClientId),
      BadRequest(
        toJson(
          ErrorResponse(
            uuidService.uuid,
            BadRequestCode,
            MissingHeaderClientId
          )
        )
      )
    )

}
