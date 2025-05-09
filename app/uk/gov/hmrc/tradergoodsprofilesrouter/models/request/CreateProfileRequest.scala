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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.request

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Json, OWrites, Reads}
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.Reads.{lengthBetween, validActorId, validNiphl}

case class CreateProfileRequest(
  actorId: String,
  ukimsNumber: String,
  nirmsNumber: Option[String],
  niphlNumber: Option[String]
)

object CreateProfileRequest {

  implicit val reads: Reads[CreateProfileRequest] =
    ((JsPath \ "actorId").read(validActorId) and
      (JsPath \ "ukimsNumber").read(lengthBetween(32, 32)) and
      (JsPath \ "nirmsNumber").readNullable(lengthBetween(13, 13)) and
      (JsPath \ "niphlNumber").readNullable(validNiphl))(CreateProfileRequest.apply)

  implicit val writes: OWrites[CreateProfileRequest] =
    OWrites[CreateProfileRequest] { request =>
      Json.obj(
        "actorId"     -> request.actorId,
        "ukimsNumber" -> request.ukimsNumber,
        "nirmsNumber" -> request.nirmsNumber,
        "niphlNumber" -> request.niphlNumber
      )
    }
}
