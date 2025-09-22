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
import play.api.libs.json._
import uk.gov.hmrc.tradergoodsprofilesrouter.models.filters.NiphlNumberFilter

case class ProfileResponse(
  eori: String,
  actorId: String,
  ukimsNumber: Option[String],
  nirmsNumber: Option[String],
  niphlNumber: Option[String]
)

object ProfileResponse extends NiphlNumberFilter {
  implicit val reads: Reads[ProfileResponse] = (
    (JsPath \ "eori").read[String] and
      (JsPath \ "actorId").read[String] and
      (JsPath \ "ukimsNumber").readNullable[String] and
      (JsPath \ "nirmsNumber").readNullable[String] and
      (JsPath \ "niphlNumber").readNullable[String]
  )(ProfileResponse.apply _)

  implicit val writes: Writes[ProfileResponse] = (
    (JsPath \ "eori").write[String] and
      (JsPath \ "actorId").write[String] and
      (JsPath \ "ukimsNumber").writeNullable[String] and
      (JsPath \ "nirmsNumber").writeNullable[String] and
      (JsPath \ "niphlNumber").writeNullable[String]
  )(p =>
    (
      p.eori,
      p.actorId,
      p.ukimsNumber,
      p.nirmsNumber,
      removeLeadingDashes(
        p.niphlNumber
      )
    )
  )

}
