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

import play.api.libs.functional.syntax.{toApplicativeOps, toFunctionalBuilderOps}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Reads.verifying
import play.api.libs.json.{JsPath, OWrites, Reads}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.Assessment
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ValidationSupport.Reads.{lengthBetween, validActorId, validComcode}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ValidationSupport.isValidCountryCode

import java.time.Instant
import scala.Function.unlift

case class CreateRecordRequest(
  eori: String,
  actorId: String,
  traderRef: String,
  comcode: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Int,
  assessments: Option[Seq[Assessment]],
  supplementaryUnit: Option[Int],
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant]
)

object CreateRecordRequest {

  implicit val reads: Reads[CreateRecordRequest] =
    ((JsPath \ "eori").read(lengthBetween(14, 17)) and
      (JsPath \ "actorId").read(validActorId) and
      (JsPath \ "traderRef").read(lengthBetween(1, 512)) and
      (JsPath \ "comcode").read(validComcode) and
      (JsPath \ "goodsDescription").read(lengthBetween(1, 512)) and
      (JsPath \ "countryOfOrigin").read(lengthBetween(1, 2).andKeep(verifying(isValidCountryCode))) and
      (JsPath \ "category").read[Int](verifying[Int](category => category >= 1 && category <= 3)) and
      (JsPath \ "assessments").readNullable[Seq[Assessment]] and
      (JsPath \ "supplementaryUnit").readNullable[Int](
        verifying[Int](supplementaryUnit => supplementaryUnit >= -2147483648 && supplementaryUnit <= 2147483647)
      ) and
      (JsPath \ "measurementUnit").readNullable(lengthBetween(1, 255)) and
      (JsPath \ "comcodeEffectiveFromDate").read[Instant] and
      (JsPath \ "comcodeEffectiveToDate")
        .readNullable[Instant])(CreateRecordRequest.apply _)

  implicit lazy val writes: OWrites[CreateRecordRequest] =
    ((JsPath \ "eori").write[String] and
      (JsPath \ "actorId").write[String] and
      (JsPath \ "traderRef").write[String] and
      (JsPath \ "comcode").write[String] and
      (JsPath \ "goodsDescription").write[String] and
      (JsPath \ "countryOfOrigin").write[String] and
      (JsPath \ "category").write[Int] and
      (JsPath \ "assessments").writeNullable[Seq[Assessment]] and
      (JsPath \ "supplementaryUnit").writeNullable[Int] and
      (JsPath \ "measurementUnit").writeNullable[String] and
      (JsPath \ "comcodeEffectiveFromDate").write[Instant] and
      (JsPath \ "comcodeEffectiveToDate").writeNullable[Instant])(unlift(CreateRecordRequest.unapply))
}
