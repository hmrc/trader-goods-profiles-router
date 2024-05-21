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

package uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action

import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.BadRequest
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService

import scala.concurrent.ExecutionContext

class ValidateHeaderClientIdActionSpec
    extends PlaySpec
    with BeforeAndAfterEach
    with ScalaFutures
    with EitherValues
    with IntegrationPatience {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val uuidService = mock[UuidService]
  private val sut         = new ValidateHeaderClientId(uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(uuidService)
    when(uuidService.uuid).thenReturn("8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f")
  }

  "Validate Header Action" should {

    "return None for valid Client ID" in {
      val headers = FakeHeaders(Seq("X-Client-ID" -> "TSS"))
      val request =
        FakeRequest[JsValue]("remove", "uri", headers, Json.obj("test" -> "test"))
      val result  = sut.validateClientId(request)

      whenReady(result.value)(_.value mustBe "TSS")
    }
    "return a bad request" when {

      "client ID header is missing" in {
        val headers = FakeHeaders(Seq.empty)
        val request =
          FakeRequest[JsValue]("remove", "uri", headers, Json.obj("test" -> "test"))

        val result  = sut.validateClientId(request)

        whenReady(result.value)(
          _.left.value mustBe BadRequest(
            Json.toJson(
              ErrorResponse(
                "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
                "BAD_REQUEST",
                "Missing mandatory header X-Client-ID"
              )
            )
          )
        )
      }
    }
  }
}
