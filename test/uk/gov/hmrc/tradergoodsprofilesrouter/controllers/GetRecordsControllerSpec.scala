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
import org.mockito.Mockito.{RETURNS_DEEP_STUBS, verify}
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{GetRecordsService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class GetRecordsControllerSpec extends PlaySpec with MockitoSugar with GetRecordsDataSupport with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val getRecordsService = mock[GetRecordsService]
  private val uuidService       = mock[UuidService]
  private val eoriNumber        = "GB123456789001"
  private val correlationId     = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  private val recordId          = UUID.randomUUID().toString
  private val appConfig         = mock[AppConfig](RETURNS_DEEP_STUBS)

  private val sut =
    new GetRecordsController(
      new FakeSuccessAuthAction(),
      stubControllerComponents(),
      getRecordsService,
      appConfig,
      uuidService
    )

  def validHeaders: Seq[(String, String)] = Seq(
    HeaderNames.ClientId -> "clientId",
    HeaderNames.Accept   -> "application/vnd.hmrc.1.0+json"
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(uuidService.uuid).thenReturn(correlationId)
    when(appConfig.hawkConfig.getRecordsMaxSize).thenReturn(500)
    when(appConfig.hawkConfig.getRecordsDefaultSize).thenReturn(500)
  }
  "getTGPRecord" should {

    "return a successful JSON response for a single record" in {

      when(getRecordsService.fetchRecord(any, any, any)(any))
        .thenReturn(Future.successful(Right(getResponseDataWithAdviceStatus())))

      when(appConfig.hawkConfig.getRecordsUrl).thenReturn("/url")

      val result = sut.getTGPRecord("GB123456789001", recordId)(
        FakeRequest().withHeaders(validHeaders: _*)
      )
      status(result) mustBe OK
      verify(appConfig.hawkConfig).getRecordsUrl
      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(getResponseDataWithAdviceStatus())
      }
    }

    "return 400 Bad request when mandatory request header Accept is missing" in {

      val result = sut.getTGPRecord("eori", recordId)(
        FakeRequest().withHeaders(validHeaders.filterNot { case (name, _) => name.equalsIgnoreCase("Accept") }: _*)
      )
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe createMissingAcceptHeaderErrorResponse
    }

    "return an error if cannot fetch a record" in {
      val errorResponseJson =
        EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error"))

      when(getRecordsService.fetchRecord(any, any, any)(any))
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

    "return OK without validating the X-Client-Id when drop_1_1_enabled flag is true" in {
      when(appConfig.isDrop1_1_enabled).thenReturn(true)
      when(getRecordsService.fetchRecord(any, any, any)(any))
        .thenReturn(Future.successful(Right(getResponseDataWithAdviceStatus())))

      val result = sut.getTGPRecord("GB123456789001", recordId)(FakeRequest().withHeaders(validHeaders.filterNot {
        case (name, _) =>
          name.equalsIgnoreCase("X-Client-ID")
      }: _*))
      status(result) mustBe OK
    }

    // TODO: After Drop 1.1 this should be removed - Ticket: TGP-2014
    "return OK validating the the X-Client-Id when drop_1_1_enabled flag is false" in {
      when(appConfig.isDrop1_1_enabled).thenReturn(false)

      val result = sut.getTGPRecord("GB123456789001", recordId)(FakeRequest().withHeaders(validHeaders: _*))
      status(result) mustBe OK
    }
  }

  "getTGPRecords" should {

    "return a successful JSON response for a multiple records with optional query parameters" in {

      when(getRecordsService.fetchRecords(any, any, any, any)(any))
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

      when(getRecordsService.fetchRecords(any, any, any, any)(any))
        .thenReturn(Future.successful(Right(getMultipleRecordResponseData(eoriNumber))))

      val result = sut.getTGPRecords(eoriNumber)(
        FakeRequest().withHeaders(validHeaders: _*)
      )
      status(result) mustBe OK
      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(getMultipleRecordResponseData(eoriNumber))
      }
    }

    // TODO: After Drop 1.1 this should be removed - Ticket: TGP-2014
    "return OK validating the the X-Client-Id when drop_1_1_enabled flag is false" in {
      when(appConfig.isDrop1_1_enabled).thenReturn(false)

      val result = sut.getTGPRecords(eoriNumber)(FakeRequest().withHeaders(validHeaders: _*))
      status(result) mustBe OK
    }

    "return OK without validating the X-Client-Id when drop_1_1_enabled flag is true" in {
      when(appConfig.isDrop1_1_enabled).thenReturn(true)

      val result = sut.getTGPRecords(eoriNumber)(FakeRequest().withHeaders(validHeaders.filterNot { case (name, _) =>
        name.equalsIgnoreCase("X-Client-ID")
      }: _*))

      status(result) mustBe OK
    }

    "return an error" when {
      "if cannot fetch a records" in {
        val errorResponseJson =
          EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error"))

        when(getRecordsService.fetchRecords(any, any, any, any)(any))
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
        when(getRecordsService.fetchRecords(any, any, any, any)(any))
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

      "size is more than allowed max size" in {
        when(getRecordsService.fetchRecords(any, any, any, any)(any))
          .thenReturn(Future.successful(Right(getMultipleRecordResponseData())))

        val result = sut.getTGPRecords(eoriNumber, None, Some(1), Some(600))(
          FakeRequest().withHeaders(validHeaders: _*)
        )
        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> s"$correlationId",
          "code"          -> "030",
          "message"       -> "Invalid query parameter size, max allowed size is : 500"
        )
      }
    }

  }

  private def createMissingAcceptHeaderErrorResponse =
    Json.obj(
      "correlationId" -> correlationId,
      "code"          -> "BAD_REQUEST",
      "message"       -> "Bad Request",
      "errors"        -> Json.arr(
        Json.obj(
          "code"        -> "INVALID_HEADER",
          "message"     -> "Accept was missing from Header or is in the wrong format",
          "errorNumber" -> 4
        )
      )
    )

}
