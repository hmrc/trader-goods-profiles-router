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

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsPath, OWrites, Reads}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ValidationSupport.Reads.{lengthBetween, validActorId}

case class MaintainProfileRequest(
  actorId: String,
  ukimsNumber: Option[String],
  nirmsNumber: Option[String],
  niphlNumber: Option[String]
)

object MaintainProfileRequest {

  implicit val reads: Reads[MaintainProfileRequest] =
    ((JsPath \ "actorId").read(validActorId) and
      (JsPath \ "ukimsNumber").readNullable(lengthBetween(32, 32)) and
      (JsPath \ "nirmsNumber").readNullable(lengthBetween(13, 13)) and
      (JsPath \ "niphlNumber")
        .readNullable(lengthBetween(8, 8)))(MaintainProfileRequest.apply _)

  implicit lazy val writes: OWrites[MaintainProfileRequest] =
    ((JsPath \ "actorId").write[String] and
      (JsPath \ "ukimsNumber").writeNullable[String] and
      (JsPath \ "nirmsNumber").writeNullable[String] and
      (JsPath \ "niphlsNumber").writeNullable[String])(unlift(MaintainProfileRequest.unapply))
}