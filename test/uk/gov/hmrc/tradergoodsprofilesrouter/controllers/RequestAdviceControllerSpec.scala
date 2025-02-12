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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atLeastOnce, reset, verify, when}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.writeableOf_JsValue
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, CREATED}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RequestAdviceService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.*
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames
import cats.implicits._


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
    HeaderNames.ClientId -> "clientId",
    HeaderNames.Accept   -> "application/vnd.hmrc.1.0+json"
  )

  private val requestAccreditationData: JsValue = Json.parse(
    """
      |{
      |    "actorId": "GB9876543210983",
      |    "requestorName": "Mr. Phil Edwards",
      |    "requestorEmail": "Phil.Edwards@gmail.com"
      |}
      |""".stripMargin
  )

  private val invalidRequestAccreditationData: JsValue = Json.parse(
    """
      |{
      |    "actorId": "",
      |    "requestorName": "Mr. Phil Edwards",
      |    "requestorEmail": ""
      |}
      |""".stripMargin
  )

  "POST /createaccreditation" should {
    "return a 201 Created response on creating accreditation" in {
      when(mockRequestAdviceService.requestAdvice(any, any, any)(any))
        .thenReturn(EitherT.rightT[scala.concurrent.Future, ErrorResponse](CREATED))

      val result = sut.requestAdvice(eori, recordId)(
        FakeRequest().withBody(requestAccreditationData).withHeaders(validHeaders: _*)
      )

      status(result) mustBe CREATED
    }

    "return a 400 Bad Request when clientId is missing" in {
      val headersWithoutClientId = validHeaders.filterNot { case (name, _) => name.equalsIgnoreCase(HeaderNames.ClientId) }

      val result = sut.requestAdvice(eori, recordId)(
        FakeRequest().withBody(requestAccreditationData).withHeaders(headersWithoutClientId: _*)
      )

      status(result) mustBe BAD_REQUEST
    }

    "return a 400 Bad Request when Accept header is missing" in {
      val headersWithoutAccept = validHeaders.filterNot { case (name, _) => name.equalsIgnoreCase(HeaderNames.Accept) }

      val result = sut.requestAdvice(eori, recordId)(
        FakeRequest().withBody(requestAccreditationData).withHeaders(headersWithoutAccept: _*)
      )

      status(result) mustBe BAD_REQUEST
    }

    "return 400 Bad Request when recordId path variable is not valid" in {
      val invalidRecordId = "2138748712364"

      val errorResponse = ErrorResponse(
        recordId,
        BadRequestCode,
        BadRequestMessage,
        Some(
          Seq(
            Error("INVALID_QUERY_PARAMETER", "The recordId has been provided in the wrong format", 25)
          )
        )
      )

      when(mockUuidService.uuid).thenReturn(recordId)

      val result =
        sut.requestAdvice(eori, invalidRecordId)(
          FakeRequest().withBody(requestAccreditationData).withHeaders(validHeaders: _*)
        )

      status(result) mustBe BAD_REQUEST

      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(errorResponse)
      }
    }

    "return 400 Bad Request when invalid body is provided" in {
      val errorResponse = ErrorResponse(
        recordId,
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

      when(mockUuidService.uuid).thenReturn(recordId)

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
