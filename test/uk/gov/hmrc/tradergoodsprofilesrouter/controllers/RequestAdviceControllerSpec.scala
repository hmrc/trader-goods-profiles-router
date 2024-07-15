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
import play.api.http.Status.{BAD_REQUEST, CREATED}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RequestAdviceService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames._

import scala.concurrent.ExecutionContext

class RequestAdviceControllerSpec extends PlaySpec with MockitoSugar with GetRecordsDataSupport {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val eori     = "GB1234567890"
  private val recordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  private val mockRequestAdviceService = mock[RequestAdviceService]
  private val mockUuidService          = mock[UuidService]

  private val sut =
    new RequestAdviceController(
      new FakeSuccessAuthAction(),
      stubControllerComponents(),
      mockRequestAdviceService,
      mockUuidService
    )

  def validHeaders: Seq[(String, String)] = Seq(
    Accept   -> "application/json",
    ClientId -> "clientId"
  )

  private val requestAccreditationData = Json
    .parse("""
        |{
        |    "actorId": "GB9876543210983",
        |    "requestorName": "Mr. Phil Edwards",
        |    "requestorEmail": "Phil.Edwards@gmail.com"
        |}
        |""".stripMargin)

  private val invalidRequestAccreditationData = Json
    .parse("""
        |{
        |    "actorId": "",
        |    "requestorName": "Mr. Phil Edwards",
        |    "requestorEmail": ""
        |}
        |""".stripMargin)

  "POST /createaccreditation" should {

    "return a 201 Ok response on creating accreditation" in {
      when(mockRequestAdviceService.requestAdvice(any, any, any)(any))
        .thenReturn(EitherT.rightT(CREATED))

      val result = sut.requestAdvice(eori, recordId)(
        FakeRequest().withBody(requestAccreditationData).withHeaders(validHeaders: _*)
      )

      status(result) mustBe CREATED
    }

    "return a 400 Bad request when clientId is missing" in {
      def headers: Seq[(String, String)] = Seq(
        Accept -> "application/json"
      )

      val result = sut.requestAdvice(eori, recordId)(
        FakeRequest().withBody(requestAccreditationData).withHeaders(headers: _*)
      )

      status(result) mustBe BAD_REQUEST
    }

    "return 400 Bad request when recordId path variable is not valid" in {
      val errorResponse = ErrorResponse(
        "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        BadRequestCode,
        BadRequestMessage,
        Some(
          Seq(
            Error("INVALID_QUERY_PARAMETER", "The recordId has been provided in the wrong format", 25)
          )
        )
      )

      when(mockUuidService.uuid).thenReturn("8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f")
      val result =
        sut.requestAdvice(eori, "2138748712364")(
          FakeRequest().withBody(requestAccreditationData).withHeaders(validHeaders: _*)
        )

      status(result) mustBe BAD_REQUEST

      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(errorResponse)
      }
    }

    "return 400 Bad request when invalid body is provided" in {
      val errorResponse = ErrorResponse(
        "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        BadRequestCode,
        BadRequestMessage,
        Some(
          Seq(
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Mandatory field RequestorEmail was missing from body or is in the wrong format",
              38
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
        sut.requestAdvice(eori, recordId)(
          FakeRequest().withBody(invalidRequestAccreditationData).withHeaders(validHeaders: _*)
        )

      status(result) mustBe BAD_REQUEST

      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(errorResponse)
      }
    }

  }

}
