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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.audit

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.request.AuditGetRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.response.AuditGetRecordsResponse

case class AuditGetRecordsDetails(
  clientId: Option[String],
  requestDateTime: String,
  responseDateTime: String,
  outcome: AuditOutcome,
  request: AuditGetRecordRequest,
  response: AuditGetRecordsResponse
)

object AuditGetRecordsDetails {
  implicit val format: OFormat[AuditGetRecordsDetails] = Json.format[AuditGetRecordsDetails]
}

case class AuditGetRecordsFailureDetails(
  clientId: Option[String],
  requestDateTime: String,
  responseDateTime: String,
  outcome: AuditOutcome,
  request: AuditGetRecordRequest
)

object AuditGetRecordsFailureDetails {
  implicit val format: OFormat[AuditGetRecordsFailureDetails] = Json.format[AuditGetRecordsFailureDetails]
}
