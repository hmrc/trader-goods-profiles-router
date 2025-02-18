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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

case class CreateProfileResponse(
  eori: String,
  actorId: String,
  ukimsNumber: Option[String],
  nirmsNumber: Option[String],
  niphlNumber: Option[String]
)

trait NiphlNumberFilter {
  def removeLeadingDashes(niphl: Option[String]): Option[String] =
    niphl.map(_.stripPrefix("-"))
}

object CreateProfileResponse extends NiphlNumberFilter {

  implicit val reads: Reads[CreateProfileResponse] =
    (
      (JsPath \ "eori").read[String] and
        (JsPath \ "actorId").read[String] and
        (JsPath \ "ukimsNumber").readNullable[String] and
        (JsPath \ "nirmsNumber").readNullable[String] and
        (JsPath \ "niphlNumber").readNullable[String]
    )(CreateProfileResponse.apply _)

  implicit lazy val writes: OWrites[CreateProfileResponse] = new OWrites[CreateProfileResponse] {
    def writes(response: CreateProfileResponse): JsObject = Json.obj(
      "eori"        -> response.eori,
      "actorId"     -> response.actorId,
      "ukimsNumber" -> response.ukimsNumber,
      "nirmsNumber" -> response.nirmsNumber,
      "niphlNumber" -> removeLeadingDashes(response.niphlNumber)
    )
  }
}
