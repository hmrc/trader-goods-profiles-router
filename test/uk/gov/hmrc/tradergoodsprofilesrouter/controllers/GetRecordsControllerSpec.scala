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

import org.scalatestplus.play.PlaySpec
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{
  GET,
  contentAsJson,
  contentType,
  defaultAwaitTimeout,
  status,
  stubControllerComponents
}
import play.api.test.{FakeRequest, Helpers}

import scala.concurrent.Future
import scala.language.postfixOps

class GetRecordsControllerSpec extends PlaySpec {

  "GetRecordsController" should {

    "return the correct JSON response for a given EORI number" in {
      val controller = new GetRecordsController(stubControllerComponents())
      val eori = "GB123456789012"
      val result =
        controller.getRecords(eori)(FakeRequest(GET, s"/get-records/$eori"))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("eori" -> eori)
    }
  }
}
