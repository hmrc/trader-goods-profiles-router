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
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.Reads.lengthBetween
import uk.gov.hmrc.tradergoodsprofilesrouter.models.ResponseModelSupport.removeNulls

case class Assessment(
  assessmentId: Option[String] = None,
  primaryCategory: Option[Int] = None,
  condition: Option[Condition] = None
)

object Assessment {

  implicit val reads: Reads[Assessment] =
    ((JsPath \ "assessmentId").readNullable(lengthBetween(1, 35)) and
      (JsPath \ "primaryCategory").readNullable[Int] and
      (JsPath \ "condition").readNullable[Condition])(Assessment.apply _)

  implicit lazy val writes: Writes[Assessment] = (assessment: Assessment) =>
    removeNulls(
      Json.obj(
        "assessmentId"    -> assessment.assessmentId,
        "primaryCategory" -> assessment.primaryCategory,
        "condition"       -> assessment.condition
      )
    )
}
