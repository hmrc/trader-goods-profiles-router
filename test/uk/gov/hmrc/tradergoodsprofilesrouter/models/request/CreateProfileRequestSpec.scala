/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, Json}

class CreateProfileRequestSpec extends AnyWordSpec with Matchers {

  "CreateProfileRequest" should {

    "deserialize valid JSON correctly" in {
      val json = Json.parse(
        """{
          |  "actorId": "GB123456789012",
          |  "ukimsNumber": "12345678901234567890123456789012",
          |  "nirmsNumber": "1234567890123",
          |  "niphlNumber": "AB12345"
          |}""".stripMargin
      )

      val result = json.validate[CreateProfileRequest]

      result mustBe a[JsSuccess[_]]
      result.get mustBe CreateProfileRequest(
        actorId = "GB123456789012",
        ukimsNumber = "12345678901234567890123456789012",
        nirmsNumber = Some("1234567890123"),
        niphlNumber = Some("AB12345")
      )
    }

    "fail to deserialize when actorId is invalid" in {
      val json = Json.parse(
        """{
          |  "actorId": "INVALID",
          |  "ukimsNumber": "12345678901234567890123456789012",
          |  "nirmsNumber": "1234567890123",
          |  "niphlNumber": "AB12345"
          |}""".stripMargin
      )

      val result = json.validate[CreateProfileRequest]
      result mustBe a[JsError]
    }

    "fail to deserialize when ukimsNumber is not exactly 32 characters long" in {
      val json = Json.parse(
        """{
          |  "actorId": "GB123456789012",
          |  "ukimsNumber": "12345",
          |  "nirmsNumber": "1234567890123",
          |  "niphlNumber": "AB12345"
          |}""".stripMargin
      )

      val result = json.validate[CreateProfileRequest]
      result mustBe a[JsError]
    }

    "fail to deserialize when nirmsNumber is not exactly 13 characters long" in {
      val json = Json.parse(
        """{
          |  "actorId": "GB123456789012",
          |  "ukimsNumber": "12345678901234567890123456789012",
          |  "nirmsNumber": "12345",
          |  "niphlNumber": "AB12345"
          |}""".stripMargin
      )

      val result = json.validate[CreateProfileRequest]
      result mustBe a[JsError]
    }

    "fail to deserialize when niphlNumber is invalid" in {
      val json = Json.parse(
        """{
          |  "actorId": "GB123456789012",
          |  "ukimsNumber": "12345678901234567890123456789012",
          |  "nirmsNumber": "1234567890123",
          |  "niphlNumber": "INVALID123"
          |}""".stripMargin
      )

      val result = json.validate[CreateProfileRequest]
      result mustBe a[JsError]
    }

    "serialize correctly" in {
      val request = CreateProfileRequest(
        actorId = "GB123456789012",
        ukimsNumber = "12345678901234567890123456789012",
        nirmsNumber = Some("1234567890123"),
        niphlNumber = Some("AB12345")
      )

      val expectedJson = Json.parse(
        """{
          |  "actorId": "GB123456789012",
          |  "ukimsNumber": "12345678901234567890123456789012",
          |  "nirmsNumber": "1234567890123",
          |  "niphlNumber": "AB12345"
          |}""".stripMargin
      )

      Json.toJson(request) mustBe expectedJson
    }
  }
}
