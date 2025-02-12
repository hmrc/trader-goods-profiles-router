/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat, OWrites}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.filters.NiphlNumberFilter

case class MaintainProfileResponse(
  eori: String,
  actorId: String,
  ukimsNumber: Option[String],
  nirmsNumber: Option[String],
  niphlNumber: Option[String]
)

object MaintainProfileResponse extends NiphlNumberFilter {

  implicit val format: OFormat[MaintainProfileResponse] = Json.format[MaintainProfileResponse]

  implicit lazy val writes: OWrites[MaintainProfileResponse] = OWrites { response =>
    Json.obj(
      "eori"        -> response.eori,
      "actorId"     -> response.actorId,
      "ukimsNumber" -> response.ukimsNumber,
      "nirmsNumber" -> response.nirmsNumber,
      "niphlNumber" -> removeLeadingDashes(response.niphlNumber) // ✅ FIXED: Apply directly
    )
  }
}
