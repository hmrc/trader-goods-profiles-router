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

package uk.gov.hmrc.tradergoodsprofilesrouter.service

import com.google.inject.Inject
import org.apache.pekko.Done
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.tradergoodsprofilesrouter.factories.{AuditCreateRecordDetails, AuditRemoveRecordDetails, AuditRemoveRecordRequest}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.{AuditOutcome, AuditUpdateRecordDetails}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.{CreateRecordRequest, UpdateRecordRequest}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import scala.concurrent.{ExecutionContext, Future}

class AuditService @Inject() (
  auditConnector: AuditConnector,
  dateTimeService: DateTimeService
)(implicit
  ec: ExecutionContext
) extends Logging {

  private val auditSource = "trader-goods-profiles-router"
  private val auditType   = "ManageGoodsRecord"

  def emitAuditRemoveRecord(
    eori: String,
    recordId: String,
    actorId: String,
    requestedDateTime: String,
    status: String,
    statusCode: Int,
    failureReason: Option[Seq[String]] = None
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] = {

    val auditDetails = AuditRemoveRecordDetails(
      clientId = hc.headers(Seq(HeaderNames.ClientId)).head._2,
      requestDateTime = requestedDateTime,
      responseDateTime = dateTimeService.timestamp.asStringSeconds,
      outcome = AuditOutcome(status, statusCode, failureReason),
      request = AuditRemoveRecordRequest(eori, recordId, actorId)
    )

    val event = ExtendedDataEvent(
      auditSource = auditSource,
      auditType = auditType,
      tags = hc.toAuditTags(),
      detail = Json.toJson(auditDetails)
    )

    auditConnector
      .sendExtendedEvent(event)
      .map { auditResult: AuditResult =>
        logger.info(s"[AuditService] - Remove record audit event status: $auditResult.")
        Done
      }
  }

  def emitAuditCreateRecord(
    createRecordRequest: CreateRecordRequest,
    requestedDateTime: String,
    status: String,
    statusCode: Int,
    failureReason: Option[Seq[String]] = None,
    createOrUpdateRecordResponse: Option[CreateOrUpdateRecordResponse] = None
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] = {
    val auditDetails = AuditCreateRecordDetails(
      clientId = hc.headers(Seq(HeaderNames.ClientId)).head._2,
      requestDateTime = requestedDateTime,
      responseDateTime = dateTimeService.timestamp.asStringSeconds,
      outcome = AuditOutcome(status, statusCode, failureReason),
      request = createRecordRequest,
      response = createOrUpdateRecordResponse
    )

    val event = ExtendedDataEvent(
      auditSource = auditSource,
      auditType = auditType,
      tags = hc.toAuditTags(),
      detail = Json.toJson(auditDetails)
    )
    auditConnector
      .sendExtendedEvent(event)
      .map { auditResult: AuditResult =>
        logger.info(s"[AuditService] - Create record audit event status: $auditResult.")
        Done
      }
  }

  def emitAuditUpdateRecord(
    updateRecordRequest: UpdateRecordRequest,
    requestedDateTime: String,
    status: String,
    statusCode: Int,
    failureReason: Option[Seq[String]] = None,
    createOrUpdateRecordResponse: Option[CreateOrUpdateRecordResponse] = None
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] = {
    val auditDetails = AuditUpdateRecordDetails(
      clientId = hc.headers(Seq(HeaderNames.ClientId)).head._2,
      requestDateTime = requestedDateTime,
      responseDateTime = dateTimeService.timestamp.asStringSeconds,
      outcome = AuditOutcome(status, statusCode, failureReason),
      request = updateRecordRequest,
      response = createOrUpdateRecordResponse
    )

    val event = ExtendedDataEvent(
      auditSource = auditSource,
      auditType = auditType,
      tags = hc.toAuditTags(),
      detail = Json.toJson(auditDetails)
    )
    auditConnector
      .sendExtendedEvent(event)
      .map { auditResult: AuditResult =>
        logger.info(s"[AuditService] - Update record audit event status: $auditResult.")
        Done
      }
  }
}
