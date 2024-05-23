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
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.mvc.Results.InternalServerError
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.{ApplicationConstants, HeaderNames}

import scala.concurrent.ExecutionContext

class GetRecordsControllerSpec extends PlaySpec with MockitoSugar with GetRecordsDataSupport with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockRouterService = mock[RouterService]
  private val mockUuidService   = mock[UuidService]
  private val eoriNumber        = "GB123456789001"
  private val correlationId     = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  private val sut =
    new GetRecordsController(
      stubControllerComponents(),
      mockRouterService,
      mockUuidService
    )

  def validHeaders: Seq[(String, String)] = Seq(
    HeaderNames.ClientId -> "clientId"
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockUuidService.uuid).thenReturn(correlationId)
  }
  "getTGPRecord" should {

    "return a successful JSON response for a single record" in {

      when(mockRouterService.fetchRecord(any, any)(any))
        .thenReturn(EitherT.rightT(getSingleRecordResponseData))

      val result = sut.getTGPRecord("GB123456789001", "12345")(
        FakeRequest().withHeaders(validHeaders: _*)
      )
      status(result) mustBe OK
      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(getSingleRecordResponseData)
      }
    }

    "return 400 Bad request when mandatory request header X-Client-ID" in {

      val result = sut.getTGPRecord("eori", "12345")(
        FakeRequest()
      )
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(createErrorResponse)
    }

    "return an error if cannot fetch a record" in {
      val errorResponseJson = Json.obj("error" -> "error")

      when(mockRouterService.fetchRecord(any, any)(any))
        .thenReturn(EitherT.leftT(InternalServerError(errorResponseJson)))

      val result = sut.getTGPRecord("GB123456789001", "12345")(
        FakeRequest().withHeaders(validHeaders: _*)
      )
      status(result) mustBe INTERNAL_SERVER_ERROR
      withClue("should return json response") {
        contentAsJson(result) mustBe errorResponseJson
      }
    }
  }

  "getTGPRecords" should {

    "return a successful JSON response for a multiple records with optional query parameters" in {

      when(mockRouterService.fetchRecords(any, any, any, any)(any))
        .thenReturn(EitherT.rightT(getMultipleRecordResponseData()))

      val result = sut.getTGPRecords(eoriNumber, Some("2021-12-17T09:30:47.456Z"), Some(1), Some(1))(
        FakeRequest().withHeaders(validHeaders: _*)
      )
      status(result) mustBe OK
      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(getMultipleRecordResponseData())
      }
    }

    "return a successful JSON response for a multiple records without optional query parameters" in {

      when(mockRouterService.fetchRecords(any, any, any, any)(any))
        .thenReturn(EitherT.rightT(getMultipleRecordResponseData(eoriNumber)))

      val result = sut.getTGPRecords(eoriNumber)(
        FakeRequest().withHeaders(validHeaders: _*)
      )
      status(result) mustBe OK
      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(getMultipleRecordResponseData(eoriNumber))
      }
    }

    "return 400 Bad request when mandatory request header X-Client-ID" in {

      val result = sut.getTGPRecords("eoriNumber")(
        FakeRequest()
      )
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(createErrorResponse)
    }

    "return an error" when {
      "if cannot fetch a records" in {
        val errorResponseJson = Json.obj("error" -> "error")

        when(mockRouterService.fetchRecords(any, any, any, any)(any))
          .thenReturn(EitherT.leftT(InternalServerError(errorResponseJson)))

        val result = sut.getTGPRecords(eoriNumber)(
          FakeRequest().withHeaders(validHeaders: _*)
        )
        status(result) mustBe INTERNAL_SERVER_ERROR
        withClue("should return json response") {
          contentAsJson(result) mustBe errorResponseJson
        }
      }

      "lastUpdateDate is not a date" in {
        when(mockRouterService.fetchRecords(any, any, any, any)(any))
          .thenReturn(EitherT.rightT(getMultipleRecordResponseData()))

        val result = sut.getTGPRecords(eoriNumber, Some("not-a-date"), Some(1), Some(1))(
          FakeRequest().withHeaders(validHeaders: _*)
        )
        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> s"$correlationId",
          "code"          -> "INVALID_QUERY_PARAMETER",
          "message"       -> "Query parameter lastUpdateDate is not a date format"
        )

      }
    }

  }

  private def createErrorResponse = {
    val errorResponse =
      ErrorResponse(
        "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        ApplicationConstants.BadRequestCode,
        ApplicationConstants.MissingHeaderClientId
      )
    errorResponse
  }

}
