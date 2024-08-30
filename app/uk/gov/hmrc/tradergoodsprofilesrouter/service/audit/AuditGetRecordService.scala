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

package uk.gov.hmrc.tradergoodsprofilesrouter.service.audit

import com.google.inject.Inject
import org.apache.pekko.Done
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.{AuditGetRecordsDetails, AuditGetRecordsFailureDetails, AuditOutcome}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.request.AuditGetRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.response.AuditGetRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames.ClientId

import scala.concurrent.{ExecutionContext, Future}

class AuditGetRecordService @Inject() (
  auditConnector: AuditConnector,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends Logging {

  private val auditSource = "trader-goods-profiles-router"
  private val auditType   = "GetGoodsRecord"

  def emitAuditGetRecord(
    requestDetails: AuditGetRecordRequest,
    requestDateTime: String,
    status: String,
    statusCode: Int,
    response: GetEisRecordsResponse
  )(implicit hc: HeaderCarrier): Future[Done] = {

    val details = AuditGetRecordsDetails(
      clientId = hc.headers(Seq(ClientId)).headOption.map(_._2),
      requestDateTime = requestDateTime,
      responseDateTime = dateTimeService.timestamp.asStringMilliSeconds,
      outcome = AuditOutcome(status, statusCode),
      request = requestDetails,
      response = AuditGetRecordsResponse(response)
    )
    val event   = ExtendedDataEvent(
      auditSource = auditSource,
      auditType = auditType,
      tags = hc.toAuditTags(),
      detail = Json.toJson(details)
    )
    auditConnector
      .sendExtendedEvent(event)
      .map { auditResult: AuditResult =>
        logger.info(s"[AuditGetRecordService] - Get Records audit event status: $auditResult.")
        Done
      }
  }

  def emitAuditGetRecordFailure(
    requestDetails: AuditGetRecordRequest,
    requestDateTime: String,
    status: String,
    statusCode: Int,
    errors: Seq[String]
  )(implicit hc: HeaderCarrier): Future[Done] = {

    val details = AuditGetRecordsFailureDetails(
      clientId = hc.headers(Seq(ClientId)).headOption.map(_._2),
      requestDateTime = requestDateTime,
      responseDateTime = dateTimeService.timestamp.asStringMilliSeconds,
      outcome = AuditOutcome(status, statusCode, Option.when(errors.nonEmpty)(errors)),
      request = requestDetails
    )

    val event = ExtendedDataEvent(
      auditSource = auditSource,
      auditType = auditType,
      tags = hc.toAuditTags(),
      detail = Json.toJson(details)
    )

    auditConnector
      .sendExtendedEvent(event)
      .map { auditResult: AuditResult =>
        logger.info(s"[AuditGetRecordService] - Get Records audit event status: $auditResult.")
        Done
      }
  }
}
