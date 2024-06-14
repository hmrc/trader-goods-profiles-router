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

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.AccreditationConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.accreditationrequests.TraderDetails

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AccreditationServiceSpec
    extends PlaySpec
    with ScalaFutures
    with EitherValues
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val accreditationConnector = mock[AccreditationConnector]
  private val uuidService            = mock[UuidService]
  private val correlationId          = UUID.randomUUID().toString
  private val traderDetails          = TraderDetails("eori", "any-name", None, "sample@sample.com", "ukims", Seq.empty)
  private val sut                    = new AccreditationService(accreditationConnector, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(accreditationConnector, uuidService)

    when(uuidService.uuid).thenReturn(correlationId)
  }
  "requestAccreditation" should {
    "return CREATED response" in {
      when(accreditationConnector.requestAccreditation(any, any)(any)).thenReturn(Future.successful(Right(OK)))

      val result = sut.requestAccreditation(traderDetails)

      whenReady(result.value) { r =>
        r.value mustBe CREATED
        verify(accreditationConnector).requestAccreditation(traderDetails, correlationId)
      }
    }

    "return an error" when {
      "connector return an error" in {
        when(accreditationConnector.requestAccreditation(any, any)(any))
          .thenReturn(Future.successful(Left(BadRequest("error"))))

        val result = sut.requestAccreditation(traderDetails)

        whenReady(result.value) {
          _.left.value mustBe BadRequest("error")
        }
      }

      "connector throws a run time exception" in {
        when(accreditationConnector.requestAccreditation(any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = sut.requestAccreditation(traderDetails)

        whenReady(result.value) {
          _.left.value mustBe InternalServerError(
            Json.obj(
              "correlationId" -> correlationId,
              "code"          -> "UNEXPECTED_ERROR",
              "message"       -> "error"
            )
          )
        }
      }
    }
  }

}
