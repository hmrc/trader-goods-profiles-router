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

import cats.data.EitherT
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.mvc.Results.InternalServerError
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidateHeaderClientId
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import scala.concurrent.ExecutionContext

class RemoveRecordControllerSpec extends PlaySpec with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockRouterService = mock[RouterService]
  private val mockUuidService   = mock[UuidService]

  private val validateClientId = new ValidateHeaderClientId(mockUuidService)
  private val sut              =
    new RemoveRecordController(
      stubControllerComponents(),
      mockRouterService,
      mockUuidService,
      validateClientId
    )

  def validHeaders: Seq[(String, String)] = Seq(
    HeaderNames.ClientId -> "clientId"
  )

  "PUT /:eori/records/:recordId" should {

    "return a 200 Ok response on removing a record" in {

      when(mockRouterService.removeRecord(any, any, any)(any, any))
        .thenReturn(EitherT.rightT(OK))

      val result = sut.remove("GB123456789001", "12345")(
        FakeRequest().withBody(removeRecordRequestData).withHeaders(validHeaders: _*)
      )

      status(result) mustBe OK
    }
    "return 400 Bad request when mandatory request header X-Client-ID" in {

      when(mockUuidService.uuid).thenReturn("8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f")
      val result = sut.remove("GB123456789001", "12345")(
        FakeRequest().withBody(removeRecordRequestData)
      )
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(createErrorResponse)
    }
    "return 400 Bad request when required request field actorId is missing" in {
      val errorResponse = ErrorResponse(
        "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        "BAD_REQUEST",
        "Bad Request",
        Some(
          Seq(
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Mandatory field actorId was missing from body or is in the wrong format",
              8
            )
          )
        )
      )

      when(mockUuidService.uuid).thenReturn("8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f")
      val result = sut.remove("GB123456789001", "12345")(
        FakeRequest().withBody(invalidRemoveRecordRequestData).withHeaders(validHeaders: _*)
      )

      status(result) mustBe BAD_REQUEST

      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(errorResponse)
      }
    }
    "return an error if cannot remove a record" in {
      val errorResponseJson = Json.obj("error" -> "error")

      when(mockRouterService.removeRecord(any, any, any)(any, any))
        .thenReturn(EitherT.leftT(InternalServerError(errorResponseJson)))

      val result = sut.remove("GB123456789001", "12345")(
        FakeRequest().withBody(removeRecordRequestData).withHeaders(validHeaders: _*)
      )

      status(result) mustBe INTERNAL_SERVER_ERROR
      withClue("should return json response") {
        contentAsJson(result) mustBe errorResponseJson
      }
    }
  }

  private def createErrorResponse =
    ErrorResponse(
      "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
      "BAD_REQUEST",
      "Missing mandatory header X-Client-ID"
    )

  lazy val removeRecordRequestData = Json
    .parse("""
             |{
             |    "actorId": "GB098765432112"
             |}
             |""".stripMargin)

  lazy val invalidRemoveRecordRequestData = Json
    .parse("""
             |{
             |    "actorId": "1234"
             |}
             |""".stripMargin)
}
