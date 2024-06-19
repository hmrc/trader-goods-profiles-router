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

import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Json, __}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService

import java.time.Instant

class AuditEventFactorySpec extends PlaySpec with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = Seq(("X-Client-ID", "TSS")))
  private val timestamp          = Instant.parse("2021-12-17T09:30:47.7896Z")
  private val eori               = "eori"
  private val recordId           = "recordId"
  private val actorId            = "actorId"
  private val dataTimeService    = mock[DateTimeService]
  private val sut                = new AuditEventFactory(dataTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(dataTimeService)
    when(dataTimeService.timestamp).thenReturn(timestamp)
  }

  "createRemoveRecord" should {
    "return a ExtendedDataEvent with no failure reason" in {
      val result = sut.createRemoveRecord(eori, recordId, actorId, timestamp.toString, "SUCCEEDED", 201)

      result.auditSource mustBe "trader-goods-profiles-router"
      result.auditType mustBe "ManageGoodsRecord"
      result.detail mustBe auditDetailJson("SUCCEEDED", 201)
    }

    "return a ExtendedDataEvent with failure reason" in {
      val failureReason = Seq("error-1", "error-2")
      val result        =
        sut.createRemoveRecord(eori, recordId, actorId, timestamp.toString, "BAD_REQUEST", 400, Some(failureReason))

      result.auditSource mustBe "trader-goods-profiles-router"
      result.auditType mustBe "ManageGoodsRecord"
      result.detail mustBe auditDetailJsonWithFailureReason("BAD_REQUEST", 400, failureReason)
    }
  }

  private def auditDetailJson(status: String, statusCode: Int) =
    Json.obj(
      "journey"          -> "RemoveRecord",
      "clientId"         -> hc.headers(Seq("X-Client-ID")).head._2,
      "requestDateTime"  -> timestamp.toString,
      "responseDateTime" -> "2021-12-17T09:30:47Z",
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
    auditDetailJson(status, statusCode)
      .transform(
        __.json.update((__ \ "outcome" \ "failureReason").json.put(Json.toJson(failureReason)))
      )
      .get

}
