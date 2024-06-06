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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.payloads

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.UpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.Assessment

import java.time.Instant

case class UpdateRecordPayload(
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

object UpdateRecordPayload {
  implicit val format: OFormat[UpdateRecordPayload] = Json.format[UpdateRecordPayload]

  def apply(eori: String, recordId: String, request: UpdateRecordRequest): UpdateRecordPayload =
    UpdateRecordPayload(
      eori = eori,
      recordId = recordId,
      actorId = request.actorId,
      traderRef = request.traderRef,
      comcode = request.comcode,
      goodsDescription = request.goodsDescription,
      countryOfOrigin = request.countryOfOrigin,
      category = request.category,
      assessments = request.assessments,
      supplementaryUnit = request.supplementaryUnit,
      measurementUnit = request.measurementUnit,
      comcodeEffectiveFromDate = request.comcodeEffectiveFromDate,
      comcodeEffectiveToDate = request.comcodeEffectiveToDate
    )
}
