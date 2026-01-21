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

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsSuccess, Json}

class RemoveRecordRequestSpec extends PlaySpec {

  "RemoveRecordRequest" must {
    "deserialize valid JSON correctly" in {
      val validJson = Json.parse("""{"actorId": "GB987654321098"}""")

      val result = validJson.validate[RemoveRecordRequest]

      result mustBe a[JsSuccess[_]]
      result.get mustBe RemoveRecordRequest("GB987654321098")
    }

    "fail to deserialize invalid JSON" in {
      val invalidJson = Json.parse("""{"actorId": "!@#invalid"}""")

      val result = invalidJson.validate[RemoveRecordRequest]

      result mustBe a[JsError]
    }
  }
}
