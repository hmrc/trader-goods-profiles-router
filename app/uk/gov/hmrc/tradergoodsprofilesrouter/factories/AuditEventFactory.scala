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
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

case class AuditEventFactory @Inject() (
  dateTimeService: DateTimeService
) {

  private val auditSource = "trader-goods-profiles-router"
  private val auditType   = "ManageGoodsRecord"

  def createRemoveRecord(
    eori: String,
    recordId: String,
    actorId: String,
    requestedDateTime: String
  )(implicit hc: HeaderCarrier): ExtendedDataEvent = {
    var outcomeJson = Json.obj()
    //if (outcome.nonEmpty) {
    //val outcomeVal = outcome.get
    outcomeJson = Json.obj(
      "status"     -> "SUCCEEDED",
      "statusCode" -> "204"
    )
    //  }

    val auditDetails =
      Json.obj(
        "journey"          -> "RemoveRecord",
        "clientId"         -> hc.headers(Seq(HeaderNames.ClientId)).head._2,
        "requestDateTime"  -> requestedDateTime,
        "responseDateTime" -> dateTimeService.timestamp.asStringSeconds,
        "request"          -> Json.obj(
          "eori"     -> eori,
          "recordId" -> recordId,
          "actorId"  -> actorId
        ),
        "outcome"          -> outcomeJson
      )

    ExtendedDataEvent(
      auditSource = auditSource,
      auditType = auditType,
      tags = hc.toAuditTags(),
      detail = auditDetails
    )
  }
}
