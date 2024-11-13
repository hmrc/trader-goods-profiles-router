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
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CorrelationId
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{DownloadTraderDataService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.FakeAuth.FakeSuccessAuthAction

import scala.concurrent.{ExecutionContext, Future}

class DownloadTraderDataControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockService     = mock[DownloadTraderDataService]
  private val mockUuidService = mock[UuidService]

  private val eori          = "GB123456789011"
  private val correlationId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  private val controller =
    new DownloadTraderDataController(
      new FakeSuccessAuthAction(),
      mockService,
      stubControllerComponents(),
      mockUuidService
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockUuidService.uuid).thenReturn(correlationId)
  }

  "download trader profile" should {
    "return a 202 accepted response, with correlationId, if the request is accepted by EIS" in {
      when(mockService.requestDownload(any)(any)).thenReturn(Future.successful(Right(CorrelationId(correlationId))))

      val result = controller.requestDataDownload(eori)(FakeRequest())
      status(result) mustBe ACCEPTED
      contentAsJson(result) mustBe Json.obj("correlationId" -> correlationId)
    }

    "return an error from the service" in {
      val errorResponse =
        EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse("1234", "INTERNAL_SERVER_ERROR", "message"))

      when(mockService.requestDownload(any)(any)).thenReturn(Future.successful(Left(errorResponse)))

      val result = controller.requestDataDownload(eori)(FakeRequest())
      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj(
        "correlationId" -> "1234",
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "message"
      )
    }
  }

}
