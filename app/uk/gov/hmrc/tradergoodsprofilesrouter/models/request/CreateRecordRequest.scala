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
import play.api.libs.json._
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.Reads.{lengthBetween, validActorId, validComcode}
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.isValidCountryCode
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.Assessment
import uk.gov.hmrc.tradergoodsprofilesrouter.models.ResponseModelSupport.removeNulls

import java.time.Instant

case class CreateRecordRequest(
  actorId: String,
  traderRef: String,
  comcode: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Option[Int] = Some(1),
  assessments: Option[Seq[Assessment]],
  supplementaryUnit: Option[BigDecimal],
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant]
)

object CreateRecordRequest {

  implicit val reads: Reads[CreateRecordRequest] =
    ((JsPath \ "actorId").read(validActorId) and
      (JsPath \ "traderRef").read(lengthBetween(1, 512)) and
      (JsPath \ "comcode").read(validComcode) and
      (JsPath \ "goodsDescription").read(lengthBetween(1, 512)) and
      (JsPath \ "countryOfOrigin").read(lengthBetween(1, 2).andKeep(verifying(isValidCountryCode))) and
      (JsPath \ "category").readNullable[Int](verifying[Int](category => category >= 1 && category <= 3)) and
      (JsPath \ "assessments").readNullable[Seq[Assessment]] and
      (JsPath \ "supplementaryUnit").readNullable[BigDecimal] and
      (JsPath \ "measurementUnit").readNullable(lengthBetween(1, 255)) and
      (JsPath \ "comcodeEffectiveFromDate").read[Instant] and
      (JsPath \ "comcodeEffectiveToDate")
        .readNullable[Instant])(CreateRecordRequest.apply _)

  implicit lazy val writes: Writes[CreateRecordRequest] = (createRecordRequest: CreateRecordRequest) =>
    removeNulls(
      Json.obj(
        "actorId"                  -> createRecordRequest.actorId,
        "traderRef"                -> createRecordRequest.traderRef,
        "comcode"                  -> createRecordRequest.comcode,
        "goodsDescription"         -> createRecordRequest.goodsDescription,
        "countryOfOrigin"          -> createRecordRequest.countryOfOrigin,
        "category"                 -> createRecordRequest.category,
        "assessments"              -> createRecordRequest.assessments,
        "supplementaryUnit"        -> createRecordRequest.supplementaryUnit,
        "measurementUnit"          -> createRecordRequest.measurementUnit,
        "comcodeEffectiveFromDate" -> createRecordRequest.comcodeEffectiveFromDate,
        "comcodeEffectiveToDate"   -> createRecordRequest.comcodeEffectiveToDate
      )
    )
}
