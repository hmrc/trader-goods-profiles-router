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
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, CREATED}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{CreateRecordService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.{ApplicationConstants, HeaderNames}

import scala.concurrent.ExecutionContext

class CreateRecordControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val createRecordService      = mock[CreateRecordService]
  val uuidService: UuidService = mock[UuidService]

  private val sut =
    new CreateRecordController(
      stubControllerComponents(),
      createRecordService,
      uuidService
    )

  def validHeaders: Seq[(String, String)] = Seq(
    HeaderNames.ClientId -> "clientId"
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(uuidService, createRecordService)
    when(uuidService.uuid).thenReturn("8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f")
  }
  "POST /records" should {

    "return a 201 with JSON response when creating a record" in {

      when(createRecordService.createRecord(any, any)(any))
        .thenReturn(EitherT.rightT(createRecordResponseData))

      val request = FakeRequest().withBody(createRecordRequestData).withHeaders(validHeaders: _*)
      val result  = sut.create("eori")(request)

      status(result) mustBe CREATED

      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(createRecordResponseData)
      }
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
              "Mandatory field traderRef was missing from body or is in the wrong format",
              9
            )
          )
        )
      )

      val request = FakeRequest().withBody(invalidCreateRecordRequestData).withHeaders(validHeaders: _*)
      val result  = sut.create("eori")(request)

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
              "Optional field type is in the wrong format",
              17
            ),
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Optional field assessmentId is in the wrong format",
              15
            ),
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Optional field primaryCategory is in the wrong format",
              16
            ),
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Optional field conditionId is in the wrong format",
              18
            ),
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Mandatory field actorId was missing from body or is in the wrong format",
              8
            )
          )
        )
      )

      val request =
        FakeRequest().withBody(invalidCreateRecordRequestDataForAssessmentArray).withHeaders(validHeaders: _*)
      val result  = sut.create("eori")(request)

      status(result) mustBe BAD_REQUEST

      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(errorResponse)
      }
    }

    "return 400 Bad request when mandatory request header X-Client-ID" in {

      val expectedErrorResponse =
        ErrorResponse(
          "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
          "BAD_REQUEST",
          "Bad Request",
          Some(
            Seq(
              Error(
                "INVALID_HEADER",
                "Missing mandatory header X-Client-ID",
                6000
              )
            )
          )
        )

      val request = FakeRequest().withBody(createRecordRequestData)
      val result  = sut.create("eori")(request)

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(expectedErrorResponse)
    }

    "return 400 Bad request when category is out of range" in {

      val errorResponse = ErrorResponse(
        "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        ApplicationConstants.BadRequestCode,
        ApplicationConstants.BadRequestMessage,
        Some(
          Seq(
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Mandatory field category was missing from body or is in the wrong format",
              14
            )
          )
        )
      )

      val request = FakeRequest().withBody(outOfRangeCategoryRequestData).withHeaders(validHeaders: _*)
      val result  = sut.create("eori")(request)

      status(result) mustBe BAD_REQUEST

      withClue("should return json response") {
        contentAsJson(result) mustBe Json.toJson(errorResponse)
      }
    }

    "return 400 Bad request when supplementaryUnit is out of range" in {

      val errorResponse = ErrorResponse(
        "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        ApplicationConstants.BadRequestCode,
        ApplicationConstants.BadRequestMessage,
        Some(
          Seq(
            Error(
              "INVALID_REQUEST_PARAMETER",
              "Optional field supplementaryUnit is in the wrong format",
              21
            )
          )
        )
      )

      val request = FakeRequest().withBody(outOfRangeSupplementaryUnitRequestData).withHeaders(validHeaders: _*)
      val result  = sut.create("eori")(request)

      status(result) mustBe BAD_REQUEST

      contentAsJson(result) mustBe Json.toJson(errorResponse)
    }
  }

  lazy val createRecordResponseData: CreateOrUpdateRecordResponse = Json
    .parse("""
             |{
             |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
             |  "eori": "GB123456789012",
             |  "actorId": "GB098765432112",
             |  "traderRef": "BAN001001",
             |  "comcode": "10410100",
             |  "accreditationStatus": "Not Requested",
             |  "goodsDescription": "Organic bananas",
             |  "countryOfOrigin": "EC",
             |  "category": 1,
             |  "supplementaryUnit": 500,
             |  "measurementUnit": "Square metre (m2)",
             |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
             |  "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
             |  "version": 1,
             |  "active": true,
             |  "toReview": false,
             |  "reviewReason": "Commodity code change",
             |  "declarable": "SPIMM",
             |  "ukimsNumber": "XIUKIM47699357400020231115081800",
             |  "nirmsNumber": "RMS-GB-123456",
             |  "niphlNumber": "6 S12345",
             |  "createdDateTime": "2024-11-18T23:20:19Z",
             |  "updatedDateTime": "2024-11-18T23:20:19Z",
             |  "assessments": [
             |    {
             |      "assessmentId": "abc123",
             |      "primaryCategory": 1,
             |      "condition": {
             |        "type": "abc123",
             |        "conditionId": "Y923",
             |        "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
             |        "conditionTraderText": "Excluded product"
             |      }
             |    }
             |  ]
             |}
             |""".stripMargin)
    .as[CreateOrUpdateRecordResponse]

  lazy val createRecordRequestData: JsValue = Json
    .parse("""
        |{
        |    "eori": "GB123456789012",
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
        |        }
        |    ],
        |    "supplementaryUnit": 500,
        |    "measurementUnit": "Square metre (m2)",
        |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
        |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
        |}
        |""".stripMargin)

  lazy val invalidCreateRecordRequestData: JsValue = Json
    .parse("""
        |{
        |    "actorId": "GB098765432112",
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

  lazy val invalidCreateRecordRequestDataForAssessmentArray: JsValue = Json
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

  lazy val outOfRangeCategoryRequestData: JsValue = Json
    .parse("""
             |{
             |    "eori": "GB123456789012",
             |    "actorId": "GB098765432112",
             |    "traderRef": "BAN001001",
             |    "comcode": "10410100",
             |    "goodsDescription": "Organic bananas",
             |    "countryOfOrigin": "EC",
             |    "category": 24,
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

  lazy val outOfRangeSupplementaryUnitRequestData: JsValue = Json
    .parse("""
             |{
             |    "eori": "GB123456789012",
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
             |        }
             |    ],
             |    "supplementaryUnit": "25Kg",
             |    "measurementUnit": "Square metre (m2)",
             |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
             |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
             |}
             |""".stripMargin)
}
