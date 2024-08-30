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

import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.tradergoodsprofilesrouter.models.ResponseModelSupport.removeNulls
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

trait BaseAuditService {

  def dateTimeService: DateTimeService
  protected val auditSource: String
  protected val auditType: String

  def createDetails(
    requestDateTime: String,
    outcome: AuditOutcome,
    request: JsValue,
    response: Option[JsValue] = None,
    journey: Option[String] = None,
  )(implicit hc: HeaderCarrier): JsObject = {
    removeNulls(
      Json.obj(
        "journey" -> journey,
        "clientId" -> hc.headers(Seq(HeaderNames.ClientId)).headOption.map(_._2),
        "requestDateTime" -> requestDateTime,
        "responseDateTime" -> dateTimeService.timestamp.asStringMilliSeconds,
        "outcome" -> Json.toJson(outcome),
        "request" -> request,
        "response" -> response
      )
    )
  }

  def createDataEvent(details: JsObject)(implicit hc: HeaderCarrier) = {
    ExtendedDataEvent(
      auditSource = auditSource,
      auditType = auditType,
      tags = hc.toAuditTags(),
      detail = Json.toJson(details)
    )
  }
}
