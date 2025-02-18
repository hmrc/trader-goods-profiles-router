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

package uk.gov.hmrc.tradergoodsprofilesrouter.factories

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsSuccess, Json}

class AuditRemoveRecordRequestSpec extends PlaySpec {

  "AuditRemoveRecordRequest" should {

    "serialize correctly" in {
      val request      = AuditRemoveRecordRequest("GB123456789012", "record-123", "actor-456")
      val expectedJson = Json.parse("""{
          |  "eori": "GB123456789012",
          |  "recordId": "record-123",
          |  "actorId": "actor-456"
          |}""".stripMargin)

      Json.toJson(request) mustBe expectedJson
    }

    "deserialize valid JSON correctly" in {
      val json = Json.parse("""{
          |  "eori": "GB123456789012",
          |  "recordId": "record-123",
          |  "actorId": "actor-456"
          |}""".stripMargin)

      json.validate[AuditRemoveRecordRequest] mustBe JsSuccess(
        AuditRemoveRecordRequest("GB123456789012", "record-123", "actor-456")
      )
    }

    "fail to deserialize invalid JSON" in {
      val invalidJson = Json.parse("""{
          |  "eori": "GB123456789012",
          |  "recordId": 12345,
          |  "actorId": "actor-456"
          |}""".stripMargin)

      invalidJson.validate[AuditRemoveRecordRequest] mustBe a[JsError]
    }
  }
}
