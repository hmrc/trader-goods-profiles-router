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
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.InternalServerErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RemoveRecordService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import scala.concurrent.{ExecutionContext, Future}

class RemoveRecordControllerSpec extends PlaySpec with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockService     = mock[RemoveRecordService]
  private val mockUuidService = mock[UuidService]

  private val eori     = "GB123456789011"
  private val actorId  = "GB123456789011"
  private val recordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  private val controller =
    new RemoveRecordController(stubControllerComponents(), mockService, mockUuidService)

  def validHeaders: Seq[(String, String)] = Seq(
    HeaderNames.ClientId -> "clientId"
  )

  "PUT /:eori/records/:recordId" should {

    "return a 200 Ok response on removing a record" in {

      when(mockService.removeRecord(any, any, any)(any))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      val result = controller.remove(eori, recordId, actorId)(
        FakeRequest().withHeaders(validHeaders: _*)
      )

      status(result) mustBe NO_CONTENT
    }
    "return 400 Bad request when mandatory request header X-Client-ID" in {
      val expectedErrorResponse =
        ErrorResponse(
          "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
          BadRequestCode,
          BadRequestMessage,
          Some(Seq(Error("INVALID_HEADER", "Missing mandatory header X-Client-ID", 6000)))
        )

      when(mockUuidService.uuid).thenReturn("8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f")
      val result = controller.remove(eori, recordId, actorId)(
        FakeRequest()
      )
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(expectedErrorResponse)
    }

    "return an error if cannot remove a record" in {
      val errorResponse = ErrorResponse("1234", "INTERNAL_SERVER_ERROR", "any-message")

      when(mockService.removeRecord(any, any, any)(any))
        .thenReturn(Future.successful(Left(InternalServerErrorResponse(errorResponse))))

      val result = controller.remove(eori, recordId, actorId)(
        FakeRequest().withHeaders(validHeaders: _*)
      )

      status(result) mustBe INTERNAL_SERVER_ERROR
      withClue("should return json response") {
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> "1234",
          "code"          -> "INTERNAL_SERVER_ERROR",
          "message"       -> "any-message"
        )
      }
    }
  }

}
