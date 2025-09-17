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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RemoveRecordService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.*
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import scala.concurrent.{ExecutionContext, Future}

class RemoveRecordControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockService     = mock[RemoveRecordService]
  private val mockUuidService = mock[UuidService]
  private val appConfig       = mock[AppConfig](RETURNS_DEEP_STUBS)

  private val eori     = "GB123456789011"
  private val actorId  = "GB123456789011"
  private val recordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  private val controller =
    new RemoveRecordController(
      new FakeSuccessAuthAction(),
      stubControllerComponents(),
      mockService,
      mockUuidService
    )

  def validHeaders: Seq[(String, String)] = Seq(
    HeaderNames.ClientId -> "clientId",
    HeaderNames.Accept   -> "application/vnd.hmrc.1.0+json"
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockService, mockUuidService, appConfig)

  }
  "remove" should {

    "return a 200 Ok response on removing a record" in {

      when(mockService.removeRecord(any, any, any)(any))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      val result = controller.remove(eori, recordId, actorId)(
        FakeRequest().withHeaders(validHeaders: _*)
      )

      status(result) mustBe NO_CONTENT
    }

    "validate headers" in {
      when(mockService.removeRecord(any, any, any)(any))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      val headersWithoutClientId: Seq[(String, String)] = validHeaders.filterNot { case (name, _) =>
        name.equalsIgnoreCase("Accept")
      }

      val result = controller.remove(eori, recordId, actorId)(
        FakeRequest().withHeaders(headersWithoutClientId: _*)
      )

      status(result) mustBe NO_CONTENT
    }

    "return an error if cannot remove a record" in {
      val errorResponse = ErrorResponse("1234", "INTERNAL_SERVER_ERROR", "any-message")

      when(mockService.removeRecord(any, any, any)(any))
        .thenReturn(Future.successful(Left(EisHttpErrorResponse(INTERNAL_SERVER_ERROR, errorResponse))))

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
