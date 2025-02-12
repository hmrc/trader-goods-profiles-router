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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.writeableOf_JsValue
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{UpdateRecordService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.CreateRecordDataSupport
import uk.gov.hmrc.tradergoodsprofilesrouter.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.{ApplicationConstants, HeaderNames}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class UpdateRecordControllerSpec
    extends PlaySpec
    with CreateRecordDataSupport
    with MockitoSugar
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val eoriNumber                   = "eori"
  private val recordId                     = UUID.randomUUID().toString
  private val updateRecordService          = mock[UpdateRecordService]
  private val mockUuidService: UuidService = mock[UuidService]
  private val appConfig                    = mock[AppConfig](RETURNS_DEEP_STUBS)

  private val sut =
    new UpdateRecordController(
      new FakeSuccessAuthAction(),
      stubControllerComponents(),
      updateRecordService,
      appConfig,
      mockUuidService
    )

  def validHeaders: Seq[(String, String)] = Seq(
    HeaderNames.ClientId -> "clientId",
    HeaderNames.Accept   -> "application/vnd.hmrc.1.0+json"
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUuidService, updateRecordService)

    when(mockUuidService.uuid).thenReturn("8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f")
    when(appConfig.sendClientId).thenReturn(true)
  }
  "PATCH /records" should {

    "return a 200 with JSON response when updating a record" in {

      when(updateRecordService.patchRecord(any, any, any)(any))
        .thenReturn(Future.successful(Right(createOrUpdateRecordResponse)))

      val result =
        sut.patch(eoriNumber, recordId)(FakeRequest().withBody(updateRecordRequestData).withHeaders(validHeaders: _*))

      status(result) mustBe OK
    }

    "return OK validating the X-Client-Id when sendClientId flag is true" in {

      when(updateRecordService.patchRecord(any, any, any)(any))
        .thenReturn(Future.successful(Right(createOrUpdateRecordResponse)))

      val result =
        sut.patch(eoriNumber, recordId)(
          FakeRequest()
            .withBody(updateRecordRequestData)
            .withHeaders(validHeaders: _*)
        )
      status(result) mustBe OK
    }

    // TODO: After Release 2 this should be removed
    "return OK validating the the X-Client-Id when sendClientId flag is false" in {
      when(appConfig.sendClientId).thenReturn(false)
      when(updateRecordService.patchRecord(any, any, any)(any))
        .thenReturn(Future.successful(Right(createOrUpdateRecordResponse)))
      val result =
        sut.patch(eoriNumber, recordId)(FakeRequest().withBody(updateRecordRequestData).withHeaders(validHeaders: _*))
      status(result) mustBe OK
    }

    "return 400 Bad request when required request field is missing" in {

      val errorResponse = ErrorResponse(
        "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        ApplicationConstants.BadRequestCode,
        ApplicationConstants.BadRequestMessage,
        Some(
          Seq(
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Mandatory field actorId was missing from body or is in the wrong format",
              8
            )
          )
        )
      )

      val result = sut.patch(eoriNumber, recordId)(
        FakeRequest().withBody(invalidUpdateRecordRequestData).withHeaders(validHeaders: _*)
      )

      status(result) mustBe BAD_REQUEST

      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(errorResponse)
      }
    }

    "return 400 Bad request when required request field is missing from assessment array" in {

      val errorResponse = ErrorResponse(
        "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        ApplicationConstants.BadRequestCode,
        ApplicationConstants.BadRequestMessage,
        Some(
          Seq(
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Optional field assessmentId is in the wrong format",
              15
            ),
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Optional field conditionId is in the wrong format",
              18
            ),
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Optional field primaryCategory is in the wrong format",
              16
            ),
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Optional field type is in the wrong format",
              17
            )
          )
        )
      )

      val result = sut.patch(eoriNumber, recordId)(
        FakeRequest().withBody(invalidUpdateRecordRequestDataForAssessmentArray).withHeaders(validHeaders: _*)
      )

      status(result) mustBe BAD_REQUEST

      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(errorResponse)
      }
    }

    "return 400 Bad request when X-Client-ID is not sent when sendClientId featureFlag is enabled" in {
      val errorResponse =
        ErrorResponse(
          "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
          ApplicationConstants.BadRequestCode,
          ApplicationConstants.BadRequestMessage,
          Some(Seq(Error("INVALID_HEADER", "Missing mandatory header X-Client-ID", 6000)))
        )

      val result = sut.patch(eoriNumber, recordId)(
        FakeRequest()
          .withBody(updateRecordRequestData)
          .withHeaders(validHeaders.filterNot { case (name, _) => name.equalsIgnoreCase("X-Client-ID") }: _*)
      )
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(errorResponse)
    }

    "return 400 Bad request when mandatory request header Accept is missing" in {
      val errorResponse =
        ErrorResponse(
          "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
          ApplicationConstants.BadRequestCode,
          ApplicationConstants.BadRequestMessage,
          Some(Seq(Error("INVALID_HEADER", "Accept was missing from Header or is in the wrong format", 4)))
        )

      val result = sut.patch(eoriNumber, recordId)(
        FakeRequest()
          .withBody(updateRecordRequestData)
          .withHeaders(validHeaders.filterNot { case (name, _) => name.equalsIgnoreCase("Accept") }: _*)
      )
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(errorResponse)
    }

    "return a Bad Request if actorId is invalid" in {
      when(updateRecordService.patchRecord(any, any, any)(any))
        .thenReturn(Future.successful(Right(createOrUpdateRecordResponse)))

      val result = sut.patch(eoriNumber, "invalid-actorId")(
        FakeRequest().withBody(updateRecordRequestData).withHeaders(validHeaders: _*)
      )

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(
        ErrorResponse(
          "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
          ApplicationConstants.BadRequestCode,
          ApplicationConstants.BadRequestMessage,
          Some(Seq(Error("INVALID_QUERY_PARAMETER", "The recordId has been provided in the wrong format", 25)))
        )
      )
    }
  }

  val updateRecordRequestData: JsValue =
    Json.parse("""
        |{
        |    "eori": "GB123456789001",
        |    "actorId": "GB098765432112",
        |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        |    "traderRef": "BAN001001",
        |    "comcode": "10410100",
        |    "goodsDescription": "Organic bananas",
        |    "countryOfOrigin": "EC",
        |    "category": 1,
        |    "assessments": [
        |        {
        |            "assessmentId": "abc123",
        |            "primaryCategory": 1,
        |            "condition": {
        |                "type": "abc123",
        |                "conditionId": "Y923",
        |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
        |                "conditionTraderText": "Excluded product"
        |            }
        |        }
        |    ],
        |    "supplementaryUnit": 500,
        |    "measurementUnit": "Square metre (m2)",
        |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
        |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
        |}
        |""".stripMargin)

  lazy val invalidUpdateRecordRequestData: JsValue = Json
    .parse("""
        |{
        |    "traderRef": "BAN001001",
        |    "comcode": "10410100",
        |    "goodsDescription": "Organic bananas",
        |    "countryOfOrigin": "EC",
        |    "category": 1,
        |    "assessments": [
        |        {
        |            "assessmentId": "abc123",
        |            "primaryCategory": 1,
        |            "condition": {
        |                "type": "abc123",
        |                "conditionId": "Y923",
        |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
        |                "conditionTraderText": "Excluded product"
        |            }
        |        }
        |    ],
        |    "supplementaryUnit": 500,
        |    "measurementUnit": "Square metre (m2)",
        |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
        |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
        |}
        |""".stripMargin)

  lazy val invalidUpdateRecordRequestDataForAssessmentArray: JsValue = Json
    .parse("""
             |{
             |    "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
             |    "actorId": "GB098765432112",
             |    "traderRef": "BAN001001",
             |    "comcode": "10410100",
             |    "goodsDescription": "Organic bananas",
             |    "countryOfOrigin": "EC",
             |    "category": 1,
             |    "assessments": [
             |        {
             |            "assessmentId": "abc123",
             |            "primaryCategory": 1,
             |            "condition": {
             |                "type": "abc123",
             |                "conditionId": "Y923",
             |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
             |                "conditionTraderText": "Excluded product"
             |            }
             |        },
             |        {
             |            "assessmentId": "",
             |            "primaryCategory": "test",
             |            "condition": {
             |                "type": "",
             |                "conditionId": "",
             |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
             |                "conditionTraderText": "Excluded product"
             |            }
             |        }
             |    ],
             |    "supplementaryUnit": 500,
             |    "measurementUnit": "Square metre (m2)",
             |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
             |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
             |}
             |""".stripMargin)

}
