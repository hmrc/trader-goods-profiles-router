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
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.RemoveNoneFromAssessmentSupport.removeEmptyAssessment
import uk.gov.hmrc.tradergoodsprofilesrouter.models.ResponseModelSupport.removeNulls
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.Assessment
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ValidationSupport.Reads.{lengthBetween, validActorId, validComcode, validDate}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ValidationSupport.isValidCountryCode

import java.time.Instant

case class UpdateRecordRequest(
  eori: String,
  recordId: String,
  actorId: String,
  traderRef: Option[String],
  comcode: Option[String],
  goodsDescription: Option[String],
  countryOfOrigin: Option[String],
  category: Option[Int],
  assessments: Option[Seq[Assessment]],
  supplementaryUnit: Option[Int],
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: Option[Instant],
  comcodeEffectiveToDate: Option[Instant]
)

object UpdateRecordRequest {

  implicit val reads: Reads[UpdateRecordRequest] =
    ((JsPath \ "eori").read(lengthBetween(14, 17)) and
      (JsPath \ "recordId").read(lengthBetween(36, 36)) and
      (JsPath \ "actorId").read(validActorId) and
      (JsPath \ "traderRef").readNullable(lengthBetween(1, 512)) and
      (JsPath \ "comcode").readNullable(validComcode) and
      (JsPath \ "goodsDescription").readNullable(lengthBetween(1, 512)) and
      (JsPath \ "countryOfOrigin").readNullable(lengthBetween(1, 2).andKeep(verifying(isValidCountryCode))) and
      (JsPath \ "category").readNullable[Int](verifying[Int](category => category >= 1 && category <= 3)) and
      (JsPath \ "assessments").readNullable[Seq[Assessment]] and
      (JsPath \ "supplementaryUnit").readNullable[Int] and
      (JsPath \ "measurementUnit").readNullable(lengthBetween(1, 255)) and
      (JsPath \ "comcodeEffectiveFromDate").readNullable[Instant](validDate) and
      (JsPath \ "comcodeEffectiveToDate")
        .readNullable[Instant](validDate))(UpdateRecordRequest.apply _)

  implicit lazy val writes: Writes[UpdateRecordRequest] = (updateRecordRequest: UpdateRecordRequest) =>
    removeNulls(
      Json.obj(
        "eori"                     -> updateRecordRequest.eori,
        "recordId"                 -> updateRecordRequest.recordId,
        "actorId"                  -> updateRecordRequest.actorId,
        "traderRef"                -> updateRecordRequest.traderRef,
        "comcode"                  -> updateRecordRequest.comcode,
        "goodsDescription"         -> updateRecordRequest.goodsDescription,
        "countryOfOrigin"          -> updateRecordRequest.countryOfOrigin,
        "category"                 -> updateRecordRequest.category,
        "assessments"              -> removeEmptyAssessment(updateRecordRequest.assessments),
        "supplementaryUnit"        -> updateRecordRequest.supplementaryUnit,
        "measurementUnit"          -> updateRecordRequest.measurementUnit,
        "comcodeEffectiveFromDate" -> updateRecordRequest.comcodeEffectiveFromDate,
        "comcodeEffectiveToDate"   -> updateRecordRequest.comcodeEffectiveToDate
      )
    )
}
