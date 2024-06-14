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

package uk.gov.hmrc.tradergoodsprofilesrouter.models

import play.api.libs.json._
import uk.gov.hmrc.tradergoodsprofilesrouter.models.RemoveNoneFromAssessmentSupport.removeEmptyAssessment
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.Assessment

import java.time.Instant

case class CreateRecordPayload(
  eori: String,
  actorId: String,
  traderRef: String,
  comcode: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Int,
  assessments: Option[Seq[Assessment]] = None,
  supplementaryUnit: Option[BigDecimal] = None,
  measurementUnit: Option[String] = None,
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant] = None
)

object CreateRecordPayload {
  implicit val format: OFormat[CreateRecordPayload] = Json.format[CreateRecordPayload]

  def apply(eori: String, incomingRequest: CreateRecordRequest): CreateRecordPayload =
    CreateRecordPayload(
      eori = eori,
      actorId = incomingRequest.actorId,
      traderRef = incomingRequest.traderRef,
      comcode = incomingRequest.comcode,
      goodsDescription = incomingRequest.goodsDescription,
      countryOfOrigin = incomingRequest.countryOfOrigin,
      category = incomingRequest.category,
      assessments = removeEmptyAssessment(incomingRequest.assessments),
      supplementaryUnit = incomingRequest.supplementaryUnit,
      measurementUnit = incomingRequest.measurementUnit,
      comcodeEffectiveFromDate = incomingRequest.comcodeEffectiveFromDate,
      comcodeEffectiveToDate = incomingRequest.comcodeEffectiveToDate
    )
}
