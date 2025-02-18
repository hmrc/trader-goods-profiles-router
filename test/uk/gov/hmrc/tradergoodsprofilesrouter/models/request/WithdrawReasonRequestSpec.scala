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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.request

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, Json}

class WithdrawReasonRequestSpec extends AnyWordSpec with Matchers {

  "WithdrawReasonRequest JSON format" should {

    "successfully deserialize valid JSON with withdrawReason" in {
      val json = Json.parse("""{ "withdrawReason": "Duplicate entry" }""")

      val result = json.validate[WithdrawReasonRequest]

      result shouldBe JsSuccess(WithdrawReasonRequest(Some("Duplicate entry")))
    }

    "successfully deserialize valid JSON without withdrawReason" in {
      val json = Json.parse("""{ }""")

      val result = json.validate[WithdrawReasonRequest]

      result shouldBe JsSuccess(WithdrawReasonRequest(None))
    }

    "fail to deserialize invalid JSON (wrong type)" in {
      val invalidJson = Json.parse("""{ "withdrawReason": 12345 }""")

      val result = invalidJson.validate[WithdrawReasonRequest]

      result shouldBe a[JsError]
    }

    "serialize to JSON correctly" in {
      val request      = WithdrawReasonRequest(Some("Incorrect data entry"))
      val expectedJson = Json.parse("""{ "withdrawReason": "Incorrect data entry" }""")

      Json.toJson(request) shouldBe expectedJson
    }

    "serialize to JSON correctly when withdrawReason is None" in {
      val request      = WithdrawReasonRequest(None)
      val expectedJson = Json.parse("""{}""")

      Json.toJson(request) shouldBe expectedJson
    }
  }
}
