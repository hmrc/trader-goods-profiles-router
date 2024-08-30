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
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.request.AuditGetRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.response.AuditGetRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.{BaseAuditService, AuditOutcome}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService

import scala.concurrent.{ExecutionContext, Future}

class AuditGetRecordService @Inject() (
  auditConnector: AuditConnector,
  override val dateTimeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends BaseAuditService with Logging {

  override val auditSource = "trader-goods-profiles-router"
  override val auditType   = "GetGoodsRecord"

  def emitAuditGetRecord(
    requestDetails: AuditGetRecordRequest,
    requestDateTime: String,
    status: String,
    statusCode: Int,
    response: GetEisRecordsResponse
  )(implicit hc: HeaderCarrier): Future[Done] = {

    val details = createDetails(
      requestDateTime = requestDateTime,
      outcome = AuditOutcome(status, statusCode),
      request = Json.toJson(requestDetails),
      response = Some(Json.toJson(AuditGetRecordsResponse(response)))
    )
    auditConnector
      .sendExtendedEvent(createDataEvent(details))
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

    val details = createDetails(
      requestDateTime = requestDateTime,
      outcome = AuditOutcome(status, statusCode, Option.when(errors.nonEmpty)(errors)),
      request = Json.toJson(requestDetails)
    )

    auditConnector
      .sendExtendedEvent(createDataEvent(details))
      .map { auditResult: AuditResult =>
        logger.info(s"[AuditGetRecordService] - Get Records audit event status: $auditResult.")
        Done
      }
  }
}
