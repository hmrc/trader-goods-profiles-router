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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class PatchRecordRequestSpec extends AnyWordSpec with Matchers {

  "PatchRecordRequest JSON Reads" should {
    val validJson = Json.parse("""{
        |  "actorId": "GB987654321098",
        |  "traderRef": "TR123"
        |}""".stripMargin)

    "successfully deserialize a valid JSON" in {

      val result = validJson.validate[PatchRecordRequest]

      result shouldBe a[JsSuccess[_]]
    }

    "fail to deserialize an invalid actorId" in {
      val invalidJson = Json.parse("""{
          |  "actorId": "GB12",
          |  "traderRef": "TR123"
          |}""".stripMargin)

      val result = invalidJson.validate[PatchRecordRequest]

      result shouldBe a[JsError]
    }

    "serialize correctly" in {
      val validRequest = PatchRecordRequest(
        actorId = "GB987654321098",
        traderRef = Some("TR123"),
        comcode = None,
        goodsDescription = None,
        countryOfOrigin = None,
        category = None,
        assessments = None,
        supplementaryUnit = None,
        measurementUnit = None,
        comcodeEffectiveFromDate = None,
        comcodeEffectiveToDate = None
      )
      val result       = Json.toJson(validRequest)
      result shouldBe validJson
    }
  }
}
