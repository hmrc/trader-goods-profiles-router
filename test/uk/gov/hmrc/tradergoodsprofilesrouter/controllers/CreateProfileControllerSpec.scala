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

import org.mockito.ArgumentMatchersSugar.any

import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.MimeTypes
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.CreateProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{CreateProfileService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import scala.concurrent.{ExecutionContext, Future}

class CreateProfileControllerSpec extends PlaySpec with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val mockCreateProfileService: CreateProfileService = mock[CreateProfileService]
  val mockUuidService: UuidService                   = mock[UuidService]

  private val sut =
    new CreateProfileController(
      new FakeSuccessAuthAction(),
      stubControllerComponents(),
      mockCreateProfileService,
      mockUuidService
    )

  def validHeaders: Seq[(String, String)] = Seq(
    HeaderNames.Accept      -> "application/vnd.hmrc.1.0+json",
    HeaderNames.ContentType -> MimeTypes.JSON
  )

  "POST /profile/create " should {
    "return a 200 ok when the call to EIS is successful to create a profile" in {

      when(mockCreateProfileService.createProfile(any, any)(any))
        .thenReturn(Future.successful(Right(createProfileResponse)))

      val result = sut.create("123456")(FakeRequest().withBody(createProfileRequest).withHeaders(validHeaders: _*))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(createProfileResponse)
    }

    "return a 400 when the Accept header is missing" in {
      val result = sut.create("123456")(FakeRequest().withBody(createProfileRequest).withHeaders())

      status(result) mustBe BAD_REQUEST
    }

    "return a 400 when mandatory fields are missing or optional fields are invalid" in {
      val errorResponse = ErrorResponse(
        "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        BadRequestCode,
        BadRequestMessage,
        Some(
          Seq(
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Optional field niphlNumber is in the wrong format",
              35
            ),
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Optional field nirmsNumber is in the wrong format",
              34
            ),
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Mandatory field ukimsNumber was missing from body or is in the wrong format",
              33
            ),
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Mandatory field actorId was missing from body or is in the wrong format",
              8
            )
          )
        )
      )

      when(mockUuidService.uuid).thenReturn("8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f")

      val result =
        sut.create("123456")(FakeRequest().withBody(invalidCreateProfileRequest).withHeaders(validHeaders: _*))

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(errorResponse)
    }
  }

  lazy val createProfileRequest: JsValue =
    Json.parse("""
        |{
        |"actorId":"GB098765432112",
        |"ukimsNumber":"XIUKIM47699357400020231115081800",
        |"nirmsNumber":"RMS-GB-123456",
        |"niphlNumber": "12345"
        |}
        |""".stripMargin)

  lazy val invalidCreateProfileRequest: JsValue =
    Json.parse("""
        |{
        |"ukimsNumber":"",
        |"nirmsNumber":"RMS-GB-12345612312312312312321",
        |"niphlNumber": "6S6"
        |}
        |""".stripMargin)

  lazy val createProfileResponse: CreateProfileResponse =
    Json
      .parse("""
        |{
        |"eori": "GB123456789012",
        |"actorId":"GB098765432112",
        |"ukimsNumber":"XIUKIM47699357400020231115081800",
        |"nirmsNumber":"RMS-GB-123456",
        |"niphlNumber": "6S123456"
        |}
        |""".stripMargin)
      .as[CreateProfileResponse]

}
