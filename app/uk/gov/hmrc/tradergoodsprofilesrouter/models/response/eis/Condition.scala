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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsNull, JsPath, Json, Reads, Writes}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.ResponseModelSupport.removeNulls
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ValidationSupport.Reads.lengthBetween

case class Condition(
  `type`: Option[String],
  conditionId: Option[String],
  conditionDescription: Option[String],
  conditionTraderText: Option[String]
)

object Condition {

  implicit val reads: Reads[Condition] =
    ((JsPath \ "type").readNullable(lengthBetween(1, 35)) and
      (JsPath \ "conditionId").readNullable(lengthBetween(1, 10)) and
      (JsPath \ "conditionDescription").readNullable(lengthBetween(1, 512)) and
      (JsPath \ "conditionTraderText").readNullable(lengthBetween(1, 512)))(Condition.apply _)

  implicit lazy val writes: Writes[Condition] = (condition: Condition) => {
    val jsonObj = removeNulls(
      Json.obj(
        "type"                 -> condition.`type`,
        "conditionId"          -> condition.conditionId,
        "conditionDescription" -> condition.conditionDescription,
        "conditionTraderText"  -> condition.conditionTraderText
      )
    )
    if (jsonObj.value.isEmpty) JsNull else jsonObj
  }
}
