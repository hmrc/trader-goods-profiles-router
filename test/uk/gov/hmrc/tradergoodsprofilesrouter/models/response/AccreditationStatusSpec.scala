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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, Json}

class AccreditationStatusSpec extends AnyWordSpec with Matchers {

  "AccreditationStatus" should {

    "serialize correctly to JSON" in {
      Json.toJson(AccreditationStatus.Approved) mustBe Json.toJson("Approved")
      Json.toJson(AccreditationStatus.Requested) mustBe Json.toJson("Requested")
      Json.toJson(AccreditationStatus.NotRequested) mustBe Json.toJson("Not Requested")
    }

    "deserialize correctly from JSON" in {
      Json.fromJson[AccreditationStatus](Json.toJson("Approved")) mustBe JsSuccess(AccreditationStatus.Approved)
      Json.fromJson[AccreditationStatus](Json.toJson("Requested")) mustBe JsSuccess(AccreditationStatus.Requested)
      Json.fromJson[AccreditationStatus](Json.toJson("Not Requested")) mustBe JsSuccess(AccreditationStatus.NotRequested)
    }

    "fail to deserialize an unknown status" in {
      val invalidJson = Json.toJson("Unknown Status")
      val result = Json.fromJson[AccreditationStatus](invalidJson)

      result mustBe a[JsError] // Expected to fail
    }

    "contain all expected values" in {
      AccreditationStatus.values must contain allElementsOf Seq(
        AccreditationStatus.NotRequested,
        AccreditationStatus.Requested,
        AccreditationStatus.InProgress,
        AccreditationStatus.InformationRequested,
        AccreditationStatus.Withdrawn,
        AccreditationStatus.Approved,
        AccreditationStatus.Rejected
      )
    }
  }
}
