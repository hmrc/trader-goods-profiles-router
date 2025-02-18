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

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.request.AuditGetRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.advicerequests.AccreditationRequest


class AccreditationRequestSpec extends PlaySpec {

  "AuditGetRecordRequest" should {

    "serialize correctly" in {
      val request = AuditGetRecordRequest("GB123456789012", Some("2024-02-10T12:00:00Z"), Some(1), Some(10), Some("record-123"))
      val expectedJson = Json.parse(
        """{
          |  "eori": "GB123456789012",
          |  "lastUpdatedDate": "2024-02-10T12:00:00Z",
          |  "page": 1,
          |  "size": 10,
          |  "recordId": "record-123"
          |}""".stripMargin)

      Json.toJson(request) mustBe expectedJson
    }

    "deserialize valid JSON correctly" in {
      val json = Json.parse(
        """{
          |  "eori": "GB123456789012",
          |  "lastUpdatedDate": "2024-02-10T12:00:00Z",
          |  "page": 1,
          |  "size": 10,
          |  "recordId": "record-123"
          |}""".stripMargin)

      json.validate[AuditGetRecordRequest] mustBe JsSuccess(AuditGetRecordRequest("GB123456789012", Some("2024-02-10T12:00:00Z"), Some(1), Some(10), Some("record-123")))
    }

    "handle missing optional fields" in {
      val json = Json.parse(
        """{
          |  "eori": "GB123456789012"
          |}""".stripMargin)

      json.validate[AuditGetRecordRequest] mustBe JsSuccess(AuditGetRecordRequest("GB123456789012", None, None, None, None))
    }

    "fail to deserialize invalid JSON" in {
      val invalidJson = Json.parse(
        """{
          |  "eori": 123456,
          |  "lastUpdatedDate": "invalid-date",
          |  "page": "not-a-number"
          |}""".stripMargin)

      invalidJson.validate[AuditGetRecordRequest] mustBe a[JsError]
    }
  }
}
