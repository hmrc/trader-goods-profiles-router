/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}

class CreateProfileEisRequestSpec extends AnyWordSpec with Matchers {

  "CreateProfileEisRequest" should {

    val validJson = Json.parse(
      """
        |{
        |  "eori": "GB123456789012",
        |  "actorId": "GB987654321098",
        |  "ukimsNumber": "12345678901234567890123456789012",
        |  "nirmsNumber": "1234567890123",
        |  "niphlNumber": "AB12345"
        |}
        |""".stripMargin
    )

    val validRequest = CreateProfileEisRequest(
      eori = "GB123456789012",
      actorId = "GB987654321098",
      ukimsNumber = Some("12345678901234567890123456789012"),
      nirmsNumber = Some("1234567890123"),
      niphlNumber = Some("AB12345")
    )

    "deserialize valid JSON correctly" in {
      val result = validJson.validate[CreateProfileEisRequest]
      result mustBe JsSuccess(validRequest)
    }

    "serialize correctly" in {
      val result = Json.toJson(validRequest)
      result mustBe validJson
    }

    "serialize without optional fields when they are None" in {
      val requestWithoutOptionalFields = CreateProfileEisRequest(
        eori = "GB123456789012",
        actorId = "GB987654321098",
        ukimsNumber = None,
        nirmsNumber = None,
        niphlNumber = None
      )

      val expectedJson = Json.parse(
        """
          |{
          |  "eori": "GB123456789012",
          |  "actorId": "GB987654321098"
          |}
          |""".stripMargin
      )

      val result = Json.toJson(requestWithoutOptionalFields)
      result mustBe expectedJson
    }
  }
}
