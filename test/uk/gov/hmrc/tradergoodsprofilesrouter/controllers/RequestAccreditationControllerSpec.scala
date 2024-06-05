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
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.{ApplicationConstants, HeaderNames}

import scala.concurrent.ExecutionContext

class RequestAccreditationControllerSpec extends PlaySpec with MockitoSugar with GetRecordsDataSupport {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockRouterService = mock[RouterService]
  private val mockUuidService   = mock[UuidService]

  private val sut =
    new RequestAccreditationController(
      stubControllerComponents(),
      mockRouterService,
      mockUuidService
    )

  def validHeaders: Seq[(String, String)] = Seq(
    HeaderNames.Accept -> "application/json"
  )

  "POST /createaccreditation" should {

    "return a 201 Ok response on creating accreditation" in {
      when(mockRouterService.fetchRecord(any, any)(any))
        .thenReturn(EitherT.rightT(getSingleRecordResponseData))
      when(mockRouterService.requestAccreditation(any)(any))
        .thenReturn(EitherT.rightT(CREATED))

      val result = sut.requestAccreditation()(
        FakeRequest().withBody(requestAccreditationData).withHeaders(validHeaders: _*)
      )

      status(result) mustBe CREATED
    }

    "return 400 Bad request when required request field is missing" in {
      when(mockRouterService.fetchRecord(any, any)(any))
        .thenReturn(EitherT.rightT(getSingleRecordResponseData))

      val errorResponse = ErrorResponse(
        "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        ApplicationConstants.BadRequestCode,
        ApplicationConstants.BadRequestMessage,
        Some(
          Seq(
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Mandatory field RequestorEmail was missing from body or is in the wrong format",
              0
            ),
            Error("INVALID_REQUEST_PARAMETER", "The recordId has been provided in the wrong format", 26)
          )
        )
      )

      when(mockUuidService.uuid).thenReturn("8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f")
      val result = sut.requestAccreditation(
        FakeRequest().withBody(invalidRequestAccreditationData).withHeaders(validHeaders: _*)
      )

      status(result) mustBe BAD_REQUEST

      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(errorResponse)
      }
    }

  }

  lazy val requestAccreditationData = Json
    .parse("""
             |{
             |    "eori": "GB987654321098",
             |    "requestorName": "Mr. Phil Edwards",
             |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
             |    "requestorEmail": "Phil.Edwards@gmail.com"
             |}
             |""".stripMargin)

  lazy val invalidRequestAccreditationData = Json
    .parse("""
             |{
             |    "eori": "GB987654321098",
             |    "requestorName": "Mr. Phil Edwards",
             |    "recordId": "",
             |    "requestorEmail": ""
             |}
             |""".stripMargin)

}
