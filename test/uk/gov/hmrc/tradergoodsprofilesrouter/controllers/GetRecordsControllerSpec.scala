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

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{
  GET,
  contentAsJson,
  contentType,
  defaultAwaitTimeout,
  status
}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector

import scala.concurrent.Future
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class GetRecordsControllerSpec extends PlaySpec with MockitoSugar {

  "GetRecordsController GET /tgp/get-record/:eori/:recordId" should {

    "return a successful JSON response for multiple records with all parameters" in {
      val mockEisConnector = mock[EISConnector]
      val controller = new GetRecordsController(
        Helpers.stubControllerComponents(),
        mockEisConnector
      )(scala.concurrent.ExecutionContext.Implicits.global)

      val eori = "GB123456789011"
      val lastUpdatedDate = Some("2024-03-26T16:14:52Z")
      val page = Some(1)
      val size = Some(10)
      val expectedJson = Json.obj(
        // TODO: Change the response
        "status" -> "success",
        "message" -> "EIS list of records retrieved successfully",
        "eori" -> eori,
        "lastUpdatedDate" -> lastUpdatedDate,
        "page" -> page,
        "size" -> size
      )

      when(
        mockEisConnector.fetchRecords(eori, None, lastUpdatedDate, page, size)
      ).thenReturn(Future.successful(expectedJson))

      val result = controller
        .getTGPRecords(eori, lastUpdatedDate, page, size)
        .apply(FakeRequest(GET, s"/tgp/get-record/$eori/"))

      status(result) mustBe OK
      contentAsJson(result) mustBe expectedJson
    }

    "return a successful JSON response for a single record" in {
      val mockEisConnector = mock[EISConnector]
      val controller = new GetRecordsController(
        Helpers.stubControllerComponents(),
        mockEisConnector
      )(scala.concurrent.ExecutionContext.Implicits.global)

      val eori = "GB123456789011"
      val recordId = "rec123"
      val expectedJson = Json.obj(
        // TODO: Change the response
        "status" -> "success",
        "message" -> "EIS record retrieved successfully",
        "eori" -> eori,
        "recordId" -> recordId
      )

      when(
        mockEisConnector.fetchRecords(eori, Some(recordId), None, None, None)
      ).thenReturn(Future.successful(expectedJson))

      val result = controller
        .getSingleTGPRecord(eori, recordId)
        .apply(FakeRequest(GET, s"/tgp/get-record/$eori/$recordId"))

      status(result) mustBe OK
      contentAsJson(result) mustBe expectedJson
    }
  }
}
