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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{CreateProfileConnector, EisHttpErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateProfileRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.CreateProfileEisRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.CreateProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CreateProfileServiceSpec extends PlaySpec with EitherValues with BeforeAndAfterEach {

  implicit val ex: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val connector     = mock[CreateProfileConnector]
  private val uuidService   = mock[UuidService]
  private val correlationId = UUID.randomUUID().toString
  private val eori          = "eori"
  private val actorId       = "actorId"
  private val ukimNumber    = "ukims-123"
  private val muirmNumber   = "nuirmNumber-123"

  private val sut = new CreateProfileService(connector, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(connector, uuidService)
    when(uuidService.uuid).thenReturn(correlationId)
  }
  "createProfile" should {
    "return a successful response" in {

      when(connector.createProfile(any, any)(any))
        .thenReturn(Future.successful(Right(200)))

      val result = await(sut.createProfile("eori", profileRequest("niphlNumber-123")))

      result.value mustBe createProfileResponse("niphlNumber-123")

      verify(connector).createProfile(createProfileEisRequest("niphlNumber-123"), correlationId)
    }

    "return an error" when {
      "EIS return an error" in {
        val eisErrorResponse = EisHttpErrorResponse(
          500,
          ErrorResponse(
            correlationId,
            "INTERNAL_SERVER_ERROR",
            "INTERNAL_SERVER_ERROR",
            Some(Seq(Error("errorCode", "error message", 123)))
          )
        )
        when(connector.createProfile(any, any)(any))
          .thenReturn(Future.successful(Left(eisErrorResponse)))

        val result = await(sut.createProfile("eori", profileRequest("niphlNumber-123")))

        result.left.value mustBe eisErrorResponse
      }

      "EIS throw" in {
        when(connector.createProfile(any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = await(sut.createProfile(eori, profileRequest("niphlNumber-123")))

        result.left.value mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, UnexpectedErrorCode, "error")
        )
      }
    }
  }

  private def createProfileEisRequest(niphlNumber: String) =
    CreateProfileEisRequest(
      eori,
      actorId,
      Some(ukimNumber),
      Some(muirmNumber),
      Some(niphlNumber)
    )

  private def createProfileResponse(niphlNumber: String) =
    CreateProfileResponse(
      eori,
      actorId,
      Some(ukimNumber),
      Some(muirmNumber),
      Some(niphlNumber)
    )

  private def profileRequest(niphlNumber: String): CreateProfileRequest =
    CreateProfileRequest(actorId, ukimNumber, Some(muirmNumber), Some(niphlNumber))
}
