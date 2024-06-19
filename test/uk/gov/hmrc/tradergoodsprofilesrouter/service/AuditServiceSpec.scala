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

import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json, __}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.tradergoodsprofilesrouter.factories.AuditEventFactory

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class AuditServiceSpec extends PlaySpec with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders = Seq(("X-Client-ID", "TSS")))

  private val auditConnector    = mock[AuditConnector]
  private val auditEventFactory = mock[AuditEventFactory]
  private val dateTime          = Instant.parse("2021-12-17T09:30:47Z").toString
  private val eori              = "GB123456789011"
  private val recordId          = "d677693e-9981-4ee3-8574-654981ebe606"
  private val actorId           = "GB123456789011"
  private val auditEvent        = extendedDataEvent(auditDetailsJson("SUCCEEDED", 204))

  val sut = new AuditService(auditConnector, auditEventFactory)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(auditConnector, auditEventFactory)

    when(auditEventFactory.createRemoveRecord(any, any, any, any, any, any, any)(any))
      .thenReturn(auditEvent)
  }

  "auditRemoveRecord" should {
    "send an event for success response" in {

      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(sut.auditRemoveRecord(eori, recordId, actorId, dateTime, "SUCCEEDED", 204))

      result mustBe Done
      verify(auditEventFactory).createRemoveRecord(eori, recordId, actorId, dateTime, "SUCCEEDED", 204)
      verify(auditConnector).sendExtendedEvent(auditEvent)
    }

    "send an event with reason failure" in {

      val auditEventWithFailure =
        extendedDataEvent(auditDetailJsonWithFailureReason("BAD_REQUEST", 400, Seq("erro-1", "error-2")))
      when(auditEventFactory.createRemoveRecord(any, any, any, any, any, any, any)(any))
        .thenReturn(auditEventWithFailure)

      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(sut.auditRemoveRecord(eori, recordId, actorId, dateTime, "BAD_REQUEST", 400))

      result mustBe Done
      verify(auditEventFactory).createRemoveRecord(eori, recordId, actorId, dateTime, "BAD_REQUEST", 400)
      verify(auditConnector).sendExtendedEvent(auditEventWithFailure)
    }

    "catch any exception" in {
      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(sut.auditRemoveRecord(eori, recordId, actorId, dateTime, "SUCCEEDED", 201))

      result mustBe Done
      verify(auditConnector).sendExtendedEvent(auditEvent)

    }

  }

  private def extendedDataEvent(auditDetails: JsValue) =
    ExtendedDataEvent(
      auditSource = "trader-goods-profiles-router",
      auditType = "ManageGoodsRecord",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )

  private def auditDetailsJson(status: String, statusCode: Int) =
    Json.obj(
      "journey"          -> "RemoveRecord",
      "clientId"         -> hc.headers(Seq("X-Client-ID")).head._2,
      "requestDateTime"  -> dateTime,
      "responseDateTime" -> dateTime,
      "request"          -> Json.obj(
        "eori"     -> eori,
        "recordId" -> recordId,
        "actorId"  -> actorId
      ),
      "outcome"          -> Json.obj(
        "status"     -> status,
        "statusCode" -> statusCode
      )
    )

  private def auditDetailJsonWithFailureReason(status: String, statusCode: Int, failureReason: Seq[String]) =
    auditDetailsJson(status, statusCode)
      .transform(
        __.json.update((__ \ "outcome" \ "failureReason").json.put(Json.toJson(failureReason)))
      )
      .get

}
