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

package uk.gov.hmrc.tradergoodsprofilesrouter.factories

import com.google.inject.Inject
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.tradergoodsprofilesrouter.factories.AuditEventFactory.{AuditDetails, AuditOutcome, AuditRequest}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

class AuditEventFactory @Inject() (
  dateTimeService: DateTimeService
) {

  private val auditSource = "trader-goods-profiles-router"
  private val auditType   = "ManageGoodsRecord"

  def createRemoveRecord(
    eori: String,
    recordId: String,
    actorId: String,
    requestedDateTime: String,
    status: String,
    statusCode: Int,
    failureReason: Option[Seq[String]] = None
  )(implicit hc: HeaderCarrier): ExtendedDataEvent = {

    val auditDetails = AuditDetails(
      clientId = hc.headers(Seq(HeaderNames.ClientId)).head._2,
      requestDateTime = requestedDateTime,
      responseDateTime = dateTimeService.timestamp.asStringSeconds,
      outcome = AuditOutcome(status, statusCode, failureReason),
      request = AuditRequest(eori, recordId, actorId)
    )

    ExtendedDataEvent(
      auditSource = auditSource,
      auditType = auditType,
      tags = hc.toAuditTags(),
      detail = Json.toJson(auditDetails)
    )
  }
}

object AuditEventFactory {

  case class AuditDetails(
    private val journey: String = "RemoveRecord",
    clientId: String,
    requestDateTime: String,
    responseDateTime: String,
    outcome: AuditOutcome,
    request: AuditRequest
  )

  object AuditDetails {
    implicit val format: OFormat[AuditDetails] = Json.format[AuditDetails]
  }

  case class AuditRequest(
    eori: String,
    recordId: String,
    actorId: String
  )

  object AuditRequest {
    implicit val format: OFormat[AuditRequest] = Json.format[AuditRequest]
  }

  case class AuditOutcome(
    status: String,
    statusCode: Int,
    failureReason: Option[Seq[String]] = None
  )

  object AuditOutcome {
    implicit val format: OFormat[AuditOutcome] = Json.format[AuditOutcome]
  }
}
