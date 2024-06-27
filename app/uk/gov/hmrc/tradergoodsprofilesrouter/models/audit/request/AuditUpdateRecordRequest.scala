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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.request

import play.api.libs.json.{Json, OFormat}

import java.time.Instant

case class AuditUpdateRecordRequest(
  eori: String,
  recordId: String,
  actorId: String,
  goodsDescription: Option[String] = None,
  traderReference: Option[String] = None,
  category: Option[Int] = None,
  commodityCode: Option[String] = None,
  countryOfOrigin: Option[String] = None,
  commodityCodeEffectiveFrom: Option[Instant] = None,
  commodityCodeEffectiveTo: Option[Instant] = None,
  supplementaryUnit: Option[BigDecimal] = None,
  measurementUnit: Option[String] = None,
  categoryAssessments: Option[Int] = None
)

object AuditUpdateRecordRequest {
  implicit val format: OFormat[AuditUpdateRecordRequest] = Json.format[AuditUpdateRecordRequest]
}
