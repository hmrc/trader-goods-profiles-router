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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{GET, contentAsJson, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector
import org.mockito.ArgumentMatchersSugar.eqTo

import scala.concurrent.Future
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class GetRecordsControllerSpec extends PlaySpec with MockitoSugar {

  "GetRecordsController GET /tgp/get-record/:eori/:recordId" should {

    "return a successful JSON response for multiple records with all parameters" in {
      val mockEisConnector = mock[EISConnector]
      val controller       = new GetRecordsController(
        Helpers.stubControllerComponents(),
        mockEisConnector
      )

      val eori            = "GB123456789011"
      val lastUpdatedDate = Some("2024-03-26T16:14:52Z")
      val page            = Some(1)
      val size            = Some(10)
      val expectedJson    = Json.obj(
        "status"          -> "success",
        "message"         -> "EIS list of records retrieved successfully",
        "eori"            -> eori,
        "lastUpdatedDate" -> lastUpdatedDate,
        "page"            -> page,
        "size"            -> size
      )

      def validHeaders: Seq[(String, String)] = Seq(
        "X-Correlation-ID" -> "3e8dae97-b586-4cef-8511-68ac12da9028"
      )

      val fakeRequest = FakeRequest(GET, s"/tgp/get-record/$eori/")
        .withHeaders(validHeaders: _*)

      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(
        mockEisConnector.fetchRecords(
          eqTo(eori),
          eqTo(lastUpdatedDate),
          eqTo(page),
          eqTo(size)
        )(any, any)
      )
        .thenReturn(Future.successful(expectedJson))

      val result = controller
        .getTGPRecords(eori, lastUpdatedDate, page, size)
        .apply(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe expectedJson
    }
  }
}
