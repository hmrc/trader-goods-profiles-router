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
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.mvc.Results.InternalServerError
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GoodsItemRecords
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.{ApplicationConstants, HeaderNames}

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class GetRecordsControllerSpec extends PlaySpec with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val mockRouterService = mock[RouterService]
  val mockUuidService   = mock[UuidService]

  private val sut =
    new GetRecordsController(
      stubControllerComponents(),
      mockRouterService,
      mockUuidService
    )

  def validHeaders: Seq[(String, String)] = Seq(
    HeaderNames.ClientId -> "clientId"
  )

  "GET /:eori/record/:recordId" should {

    "return a successful JSON response for a single record" in {

      when(mockRouterService.fetchRecord(any, any)(any, any))
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

      when(mockUuidService.uuid).thenReturn("8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f")
      val result = sut.getTGPRecord("eori", "12345")(
        FakeRequest()
      )
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(createErrorResponse)
    }

    "return an error if cannot fetch a record" in {
      val errorResponseJson = Json.obj("error" -> "error")

      when(mockRouterService.fetchRecord(any, any)(any, any))
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

  private def createErrorResponse = {
    val errorResponse =
      ErrorResponse(
        "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        ApplicationConstants.BadRequestCode,
        ApplicationConstants.MissingHeaderClientId
      )
    errorResponse
  }

  val getSingleRecordResponseData: GoodsItemRecords = Json
    .parse("""
                                                          |  {
                                                          |    "eori": "GB1234567890",
                                                          |    "actorId": "GB1234567890",
                                                          |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
                                                          |    "traderRef": "BAN001001",
                                                          |    "comcode": "104101000",
                                                          |    "accreditationRequest": "Not requested",
                                                          |    "goodsDescription": "Organic bananas",
                                                          |    "countryOfOrigin": "EC",
                                                          |    "category": 3,
                                                          |    "assessments": [
                                                          |      {
                                                          |        "assessmentId": "abc123",
                                                          |        "primaryCategory": "1",
                                                          |        "condition": {
                                                          |          "type": "abc123",
                                                          |          "conditionId": "Y923",
                                                          |          "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
                                                          |          "conditionTraderText": "Excluded product"
                                                          |        }
                                                          |      }
                                                          |    ],
                                                          |    "supplementaryUnit": 500,
                                                          |    "measurementUnit": "square meters(m^2)",
                                                          |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
                                                          |    "comcodeEffectiveToDate": "",
                                                          |    "version": 1,
                                                          |    "active": true,
                                                          |    "toReview": false,
                                                          |    "reviewReason": null,
                                                          |    "declarable": "IMMI declarable",
                                                          |    "ukimsNumber": "XIUKIM47699357400020231115081800",
                                                          |    "nirmsNumber": "RMS-GB-123456",
                                                          |    "niphlNumber": "6 S12345",
                                                          |    "locked": false,
                                                          |    "srcSystemName": "CDAP",
                                                          |    "createdDateTime": "2024-11-18T23:20:19Z",
                                                          |    "updatedDateTime": "2024-11-18T23:20:19Z"
                                                          |  }
                                                          |""".stripMargin)
    .as[GoodsItemRecords]
}
