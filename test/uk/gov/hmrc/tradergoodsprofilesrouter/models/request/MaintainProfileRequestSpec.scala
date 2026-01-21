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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, Json}

class MaintainProfileRequestSpec extends AnyWordSpec with Matchers {

  "MaintainProfileRequest" should {

    val validJson = Json.parse(
      """
        |{
        |  "actorId": "GB123456789012",
        |  "ukimsNumber": "12345678901234567890123456789012",
        |  "nirmsNumber": "1234567890123",
        |  "niphlNumber": "AB12345"
        |}
        |""".stripMargin
    )

    val validRequest = MaintainProfileRequest(
      actorId = "GB123456789012",
      ukimsNumber = "12345678901234567890123456789012",
      nirmsNumber = Some("1234567890123"),
      niphlNumber = Some("AB12345")
    )

    "deserialize valid JSON correctly" in {
      val result = validJson.validate[MaintainProfileRequest]
      result mustBe JsSuccess(validRequest)
    }

    "fail to deserialize invalid JSON" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "actorId": "INVALID_ACTOR",
          |  "ukimsNumber": "short",
          |  "nirmsNumber": "123",
          |  "niphlNumber": "AB12"
          |}
          |""".stripMargin
      )

      val result = invalidJson.validate[MaintainProfileRequest]
      result mustBe a[JsError]
    }

    "serialize correctly" in {
      val result = Json.toJson(validRequest)
      result mustBe validJson
    }

    "serialize without optional fields when they are None" in {
      val requestWithoutOptionalFields = MaintainProfileRequest(
        actorId = "GB123456789012",
        ukimsNumber = "12345678901234567890123456789012",
        nirmsNumber = None,
        niphlNumber = None
      )

      val expectedJson = Json.parse(
        """
          |{
          |  "actorId": "GB123456789012",
          |  "ukimsNumber": "12345678901234567890123456789012"
          |}
          |""".stripMargin
      )

      val result = Json.toJson(requestWithoutOptionalFields)
      result mustBe expectedJson
    }
  }
}
