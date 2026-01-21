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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, Json}

class RemoveEisRecordRequestSpec extends AnyWordSpec with Matchers {

  "RemoveEisRecordRequest" should {

    "serialize correctly" in {
      val request = RemoveEisRecordRequest(
        eori = "GB123456789012",
        recordId = "record-001",
        actorId = "ACTOR_001"
      )

      val json = Json.toJson(request)

      json mustBe Json.parse(
        """
          |{
          |  "eori": "GB123456789012",
          |  "recordId": "record-001",
          |  "actorId": "ACTOR_001"
          |}
          |""".stripMargin
      )
    }

    "deserialize correctly" in {
      val json = Json.parse(
        """
          |{
          |  "eori": "GB123456789012",
          |  "recordId": "record-001",
          |  "actorId": "ACTOR_001"
          |}
          |""".stripMargin
      )

      val result = json.validate[RemoveEisRecordRequest]

      result mustBe JsSuccess(
        RemoveEisRecordRequest(
          eori = "GB123456789012",
          recordId = "record-001",
          actorId = "ACTOR_001"
        )
      )
    }

    "fail to deserialize invalid JSON" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "eori": "",
          |  "recordId": 123,
          |  "actorId": null
          |}
          |""".stripMargin
      )

      val result = invalidJson.validate[RemoveEisRecordRequest]

      result mustBe a[JsError]
    }
  }
}
