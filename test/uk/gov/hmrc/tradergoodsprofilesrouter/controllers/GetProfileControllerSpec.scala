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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.ProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.GetProfileService
import uk.gov.hmrc.tradergoodsprofilesrouter.support.FakeAuth.FakeSuccessAuthAction

import scala.concurrent.{ExecutionContext, Future}

class GetProfileControllerSpec extends PlaySpec {

  implicit val ex: ExecutionContext = ExecutionContext.global

  val eori                      = "GB123456789001"
  private val getProfileService = mock[GetProfileService]

  private val sut = new GetProfileController(
    new FakeSuccessAuthAction(),
    stubControllerComponents(),
    getProfileService
  )

  "getProfile" should {
    "return 200" in {
      when(getProfileService.getProfile(any)(any))
        .thenReturn(Future.successful(Right(ProfileResponse(eori, "123", None, None, None))))

      val result = sut.getProfile(eori)(FakeRequest())

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

      val result = sut.getProfile(eori)(FakeRequest())

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
  }

}
