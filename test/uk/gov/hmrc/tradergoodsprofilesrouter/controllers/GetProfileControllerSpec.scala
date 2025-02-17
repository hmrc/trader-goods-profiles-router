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

package uk.gov.hmrc.tradergoodsprofilesrouter.controllers

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.ProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{GetProfileService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.FakeAuth.FakeSuccessAuthAction

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class GetProfileControllerSpec extends PlaySpec with BeforeAndAfterEach {

  implicit val ex: ExecutionContext = ExecutionContext.global

  private val eori              = "GB123456789001"
  private val correlationId     = UUID.randomUUID().toString
  private val getProfileService = mock[GetProfileService]
  private val uuidService       = mock[UuidService]

  private val sut = new GetProfileController(
    new FakeSuccessAuthAction(),
    stubControllerComponents(),
    getProfileService,
    uuidService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(getProfileService, uuidService)
    when(uuidService.uuid).thenReturn(correlationId)
  }
  "getProfile" should {
    "return 200" in {
      when(getProfileService.getProfile(any)(any))
        .thenReturn(Future.successful(Right(ProfileResponse(eori, "123", None, None, None))))

      val result = sut.getProfile(eori)(
        FakeRequest()
          .withHeaders(FakeHeaders(Seq("Accept" -> "application/vnd.hmrc.1.0+json")))
      )

      status(result) mustBe OK

      withClue("should send the request to Eis") {
        verify(getProfileService).getProfile(eqTo(eori))(any)
      }
    }

    "return an error" in {
      when(getProfileService.getProfile(any)(any))
        .thenReturn(
          Future.successful(
            Left(
              EisHttpErrorResponse(
                NOT_FOUND,
                ErrorResponse("correlationId", "code", "message", Some(Seq(Error("errorCode", "errorMessage", 6))))
              )
            )
          )
        )

      val result = sut.getProfile(eori)(
        FakeRequest()
          .withHeaders(FakeHeaders(Seq("Accept" -> "application/vnd.hmrc.1.0+json")))
      )

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe Json.obj(
        "correlationId" -> "correlationId",
        "code"          -> "code",
        "message"       -> "message",
        "errors"        -> Json.arr(
          Json.obj(
            "code"        -> "errorCode",
            "message"     -> "errorMessage",
            "errorNumber" -> 6
          )
        )
      )

    }

    "return BadRequest if Accept header is not valid" in {
      when(getProfileService.getProfile(any)(any))
        .thenReturn(Future.successful(Right(ProfileResponse(eori, "123", None, None, None))))

      val result = sut.getProfile(eori)(FakeRequest())

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "BAD_REQUEST",
        "message"       -> "Bad Request",
        "errors"        -> Json.arr(
          Json.obj(
            "code"        -> "INVALID_HEADER",
            "message"     -> "Accept was missing from Header or is in the wrong format",
            "errorNumber" -> 4
          )
        )
      )

      withClue("should not send the request to Eis") {
        verifyNoInteractions(getProfileService)
      }
    }
  }
}
