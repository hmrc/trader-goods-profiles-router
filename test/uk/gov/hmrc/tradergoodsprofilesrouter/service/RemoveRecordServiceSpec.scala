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
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.RemoveRecordConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.support.CreateRecordDataSupport

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class RemoveRecordServiceSpec
    extends PlaySpec
    with CreateRecordDataSupport
    with EitherValues
    with ScalaFutures
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val eori            = "GB123456789011"
  private val actorId         = "GB123456789011"
  private val recordId        = "12345"
  private val connector       = mock[RemoveRecordConnector]
  private val auditService    = mock[AuditService]
  private val dateTimeService = mock[DateTimeService]
  private val uuidService     = mock[UuidService]
  private val correlationId   = "1234-5678-9012"
  private val dateTime        = Instant.parse("2021-12-17T09:30:47Z")

  val service = new RemoveRecordService(connector, uuidService, auditService, dateTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(connector, uuidService)
    when(uuidService.uuid).thenReturn(correlationId)
    when(dateTimeService.timestamp).thenReturn(dateTime)
  }

  "remove a record item" in {
    when(connector.removeRecord(any, any, any, any)(any))
      .thenReturn(Future.successful(Right(OK)))
    when(auditService.auditRemoveRecord(any, any, any, any)(any)).thenReturn(Future.successful(Done))

    val result = service.removeRecord(eori, recordId, actorId)

    whenReady(result.value) { r =>
      r.value shouldBe NO_CONTENT
      verify(auditService).auditRemoveRecord(eori, recordId, actorId, dateTime.toString)

    }
  }

  "EIS return an error" in {
    when(connector.removeRecord(any, any, any, any)(any))
      .thenReturn(Future.successful(Left(BadRequest("error"))))

    val result = service.removeRecord(eori, recordId, actorId)

    whenReady(result.value) {
      _.left.value shouldBe BadRequest("error")
    }
  }

  "error when an exception is thrown" in {
    when(connector.removeRecord(any, any, any, any)(any))
      .thenReturn(Future.failed(new RuntimeException("error")))

    val result = service.removeRecord(eori, recordId, actorId)

    whenReady(result.value) {
      _.left.value shouldBe InternalServerError(
        Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "UNEXPECTED_ERROR",
          "message"       -> "error"
        )
      )
    }
  }
}
