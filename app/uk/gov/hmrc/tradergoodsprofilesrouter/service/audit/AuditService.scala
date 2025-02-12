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
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.tradergoodsprofilesrouter.factories.AuditRemoveRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.CreateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.request.AuditUpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.response.{AuditCreateRecordResponse, AuditUpdateRecordResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.{AuditCreateRecordRequest, AuditOutcome, BaseAuditService}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.payloads.UpdateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService

import scala.concurrent.{ExecutionContext, Future}


class AuditService @Inject() (
  auditConnector: AuditConnector,
  override val dateTimeService: DateTimeService
)(implicit
  ec: ExecutionContext
) extends BaseAuditService
    with Logging {

  override val auditSource = "trader-goods-profiles-router"
  override val auditType   = "ManageGoodsRecord"

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

    val auditDetails = createDetails(
      requestDateTime = requestedDateTime,
      outcome = AuditOutcome(status, statusCode, failureReason),
      request = Json.toJson(AuditRemoveRecordRequest(eori, recordId, actorId)),
      journey = Some("RemoveRecord")
    )

    auditConnector
      .sendExtendedEvent(createDataEvent(auditDetails))
      .map { (auditResult: AuditResult) =>
        logger.info(s"[AuditService] - Remove record audit event status: $auditResult.")
        Done
      }
  }

  def emitAuditCreateRecord(
    createRecordPayload: CreateRecordPayload,
    requestedDateTime: String,
    status: String,
    statusCode: Int,
    failureReason: Option[Seq[String]] = None,
    createOrUpdateRecordResponse: Option[CreateOrUpdateRecordResponse] = None
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] = {

    val auditDetails = createDetails(
      requestedDateTime,
      AuditOutcome(status, statusCode, failureReason),
      Json.toJson(prepareAuditCreateRecordRequest(createRecordPayload)),
      Some(Json.toJson(prepareAuditCreateRecordResponse(createOrUpdateRecordResponse))),
      Some("CreateRecord")
    )

    auditConnector
      .sendExtendedEvent(createDataEvent(auditDetails))
      .map { (auditResult: AuditResult) =>
        logger.info(s"[AuditService] - Create record audit event status: $auditResult.")
        Done
      }
  }

  def emitAuditUpdateRecord(
    updateRecordPayload: UpdateRecordPayload,
    requestedDateTime: String,
    status: String,
    statusCode: Int,
    failureReason: Option[Seq[String]] = None,
    createOrUpdateRecordResponse: Option[CreateOrUpdateRecordResponse] = None
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] = {

    val auditDetails = createDetails(
      requestedDateTime,
      AuditOutcome(status, statusCode, failureReason),
      Json.toJson(prepareAuditUpdateRecordRequest(updateRecordPayload)),
      Some(Json.toJson(prepareAuditUpdateRecordResponse(createOrUpdateRecordResponse))),
      Some("UpdateRecord")
    )

    auditConnector
      .sendExtendedEvent(createDataEvent(auditDetails))
      .map { (auditResult: AuditResult) =>
        logger.info(s"[AuditService] - Update record audit event status: $auditResult.")
        Done
      }
  }

  private def prepareAuditCreateRecordRequest(payload: CreateRecordPayload) =
    AuditCreateRecordRequest(
      payload.eori,
      payload.actorId,
      payload.goodsDescription,
      payload.traderRef,
      payload.category,
      payload.comcode,
      payload.countryOfOrigin,
      payload.comcodeEffectiveFromDate,
      payload.comcodeEffectiveToDate,
      payload.supplementaryUnit,
      payload.measurementUnit,
      payload.assessments.map(as => as.iterator.size)
    )

  private def prepareAuditUpdateRecordRequest(payload: UpdateRecordPayload) =
    AuditUpdateRecordRequest(
      payload.eori,
      payload.recordId,
      payload.actorId,
      payload.goodsDescription,
      payload.traderRef,
      payload.category,
      payload.comcode,
      payload.countryOfOrigin,
      payload.comcodeEffectiveFromDate,
      payload.comcodeEffectiveToDate,
      payload.supplementaryUnit,
      payload.measurementUnit,
      payload.assessments.map(as => as.iterator.size)
    )

  private def prepareAuditCreateRecordResponse(response: Option[CreateOrUpdateRecordResponse]) =
    response.map(res =>
      AuditCreateRecordResponse(
        res.recordId,
        res.adviceStatus,
        res.version,
        res.active,
        res.toReview,
        res.reviewReason,
        res.declarable,
        res.ukimsNumber,
        res.nirmsNumber,
        res.niphlNumber
      )
    )

  private def prepareAuditUpdateRecordResponse(response: Option[CreateOrUpdateRecordResponse]) =
    response.map(res =>
      AuditUpdateRecordResponse(
        res.adviceStatus,
        res.version,
        res.active,
        res.toReview,
        res.reviewReason,
        res.declarable,
        res.ukimsNumber,
        res.nirmsNumber,
        res.niphlNumber
      )
    )
}
