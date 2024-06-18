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
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.InternalServerErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{GetRecordsService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class GetRecordsControllerSpec extends PlaySpec with MockitoSugar with GetRecordsDataSupport with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val getRecordsSrvice = mock[GetRecordsService]
  private val uuidService      = mock[UuidService]
  private val eoriNumber       = "GB123456789001"
  private val correlationId    = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  private val recordId         = UUID.randomUUID().toString

  private val sut =
    new GetRecordsController(
      new FakeSuccessAuthAction(),
      stubControllerComponents(),
      getRecordsSrvice,
      uuidService
    )

  def validHeaders: Seq[(String, String)] = Seq(
    HeaderNames.ClientId -> "clientId"
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(uuidService.uuid).thenReturn(correlationId)
  }
  "getTGPRecord" should {

    "return a successful JSON response for a single record" in {

      when(getRecordsSrvice.fetchRecord(any, any)(any))
        .thenReturn(Future.successful(Right(getSingleRecordResponseData)))

      val result = sut.getTGPRecord("GB123456789001", recordId)(
        FakeRequest().withHeaders(validHeaders: _*)
      )
      status(result) mustBe OK
      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(getSingleRecordResponseData)
      }
    }

    "return 400 Bad request when mandatory request header X-Client-ID" in {

      val result = sut.getTGPRecord("eori", recordId)(
        FakeRequest()
      )
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe createMissingHeaderErrorResponse
    }

    "return an error if cannot fetch a record" in {
      val errorResponseJson = InternalServerErrorResponse(
        ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error")
      )

      when(getRecordsSrvice.fetchRecord(any, any)(any))
        .thenReturn(Future.successful(Left(errorResponseJson)))

      val result = sut.getTGPRecord("GB123456789001", recordId)(
        FakeRequest().withHeaders(validHeaders: _*)
      )
      status(result) mustBe INTERNAL_SERVER_ERROR
      withClue("should return json response") {
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "UNEXPECTED_ERROR",
          "message"       -> "error"
        )
      }
    }
  }

  "getTGPRecords" should {

    "return a successful JSON response for a multiple records with optional query parameters" in {

      when(getRecordsSrvice.fetchRecords(any, any, any, any)(any))
        .thenReturn(Future.successful(Right(getMultipleRecordResponseData())))

      val result = sut.getTGPRecords(eoriNumber, Some("2021-12-17T09:30:47.456Z"), Some(1), Some(1))(
        FakeRequest().withHeaders(validHeaders: _*)
      )
      status(result) mustBe OK
      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(getMultipleRecordResponseData())
      }
    }

    "return a successful JSON response for a multiple records without optional query parameters" in {

      when(getRecordsSrvice.fetchRecords(any, any, any, any)(any))
        .thenReturn(Future.successful(Right(getMultipleRecordResponseData(eoriNumber))))

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
      contentAsJson(result) mustBe createMissingHeaderErrorResponse
    }

    "return an error" when {
      "if cannot fetch a records" in {
        val errorResponseJson = InternalServerErrorResponse(
          ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error")
        )

        when(getRecordsSrvice.fetchRecords(any, any, any, any)(any))
          .thenReturn(Future.successful(Left(errorResponseJson)))

        val result = sut.getTGPRecords(eoriNumber)(
          FakeRequest().withHeaders(validHeaders: _*)
        )
        status(result) mustBe INTERNAL_SERVER_ERROR
        withClue("should return json response") {
          contentAsJson(result) mustBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNEXPECTED_ERROR",
            "message"       -> "error"
          )
        }
      }

      "lastUpdateDate is not a date" in {
        when(getRecordsSrvice.fetchRecords(any, any, any, any)(any))
          .thenReturn(Future.successful(Right(getMultipleRecordResponseData())))

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

  private def createMissingHeaderErrorResponse =
    Json.obj(
      "correlationId" -> correlationId,
      "code"          -> "BAD_REQUEST",
      "message"       -> "Bad Request",
      "errors"        -> Json.arr(
        Json.obj(
          "code"        -> "INVALID_HEADER",
          "message"     -> "Missing mandatory header X-Client-ID",
          "errorNumber" -> 6000
        )
      )
    )

}
