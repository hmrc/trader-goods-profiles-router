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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.tradergoodsprofilesrouter.factories.AuditEventFactory

import scala.concurrent.{ExecutionContext, Future}

class AuditService @Inject() (
  auditConnector: AuditConnector,
  auditEventFactory: AuditEventFactory
)(implicit
  ec: ExecutionContext
) extends Logging {

  def auditRemoveRecord(
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
    val event = auditEventFactory.createRemoveRecord(
      eori,
      recordId,
      actorId,
      requestedDateTime,
      status,
      statusCode,
      failureReason
    )
    auditConnector
      .sendExtendedEvent(event)
      .map { auditResult: AuditResult =>
        logger.info(s"[AuditService] - Remove record audit event status: $auditResult.")
        Done
      }
  }

}
