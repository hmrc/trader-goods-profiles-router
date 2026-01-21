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
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.Assessment

import java.time.Instant

class CreateRecordRequestSpec extends AnyWordSpec with Matchers {

  "CreateRecordRequest" should {

    "deserialize valid JSON correctly" in {
      val json = Json.parse(
        """{
          |  "actorId": "GB123456789012",
          |  "traderRef": "Trader123",
          |  "comcode": "123456",
          |  "goodsDescription": "Electronic goods",
          |  "countryOfOrigin": "GB",
          |  "category": 2,
          |  "assessments": [],
          |  "supplementaryUnit": 10.5,
          |  "measurementUnit": "kg",
          |  "comcodeEffectiveFromDate": "2024-05-12T12:15:15Z",
          |  "comcodeEffectiveToDate": "2025-05-12T12:15:15Z"
          |}""".stripMargin
      )

      val result = json.validate[CreateRecordRequest]

      result mustBe a[JsSuccess[_]]
      result.get mustBe CreateRecordRequest(
        actorId = "GB123456789012",
        traderRef = "Trader123",
        comcode = "123456",
        goodsDescription = "Electronic goods",
        countryOfOrigin = "GB",
        category = Some(2),
        assessments = Some(Seq.empty[Assessment]),
        supplementaryUnit = Some(BigDecimal(10.5)),
        measurementUnit = Some("kg"),
        comcodeEffectiveFromDate = Instant.parse("2024-05-12T12:15:15Z"),
        comcodeEffectiveToDate = Some(Instant.parse("2025-05-12T12:15:15Z"))
      )
    }

    "fail to deserialize when actorId is invalid" in {
      val json = Json.parse(
        """{
          |  "actorId": "INVALID",
          |  "traderRef": "Trader123",
          |  "comcode": "123456",
          |  "goodsDescription": "Electronic goods",
          |  "countryOfOrigin": "GB",
          |  "category": 2,
          |  "assessments": [],
          |  "supplementaryUnit": 10.5,
          |  "measurementUnit": "kg",
          |  "comcodeEffectiveFromDate": "2024-05-12T12:15:15Z"
          |}""".stripMargin
      )

      val result = json.validate[CreateRecordRequest]
      result mustBe a[JsError]
    }

    "fail to deserialize when traderRef length is out of bounds" in {
      val json = Json.parse(
        """{
          |  "actorId": "GB123456789012",
          |  "traderRef": "",
          |  "comcode": "123456",
          |  "goodsDescription": "Electronic goods",
          |  "countryOfOrigin": "GB",
          |  "category": 2,
          |  "comcodeEffectiveFromDate": "2024-05-12T12:15:15Z"
          |}""".stripMargin
      )

      val result = json.validate[CreateRecordRequest]
      result mustBe a[JsError]
    }

    "fail to deserialize when comcode is invalid" in {
      val json = Json.parse(
        """{
          |  "actorId": "GB123456789012",
          |  "traderRef": "Trader123",
          |  "comcode": "INVALID",
          |  "goodsDescription": "Electronic goods",
          |  "countryOfOrigin": "GB",
          |  "category": 2,
          |  "comcodeEffectiveFromDate": "2024-05-12T12:15:15Z"
          |}""".stripMargin
      )

      val result = json.validate[CreateRecordRequest]
      result mustBe a[JsError]
    }

    "fail to deserialize when goodsDescription is out of bounds" in {
      val json = Json.parse(
        """{
          |  "actorId": "GB123456789012",
          |  "traderRef": "Trader123",
          |  "comcode": "123456",
          |  "goodsDescription": "",
          |  "countryOfOrigin": "GB",
          |  "category": 2,
          |  "comcodeEffectiveFromDate": "2024-05-12T12:15:15Z"
          |}""".stripMargin
      )

      val result = json.validate[CreateRecordRequest]
      result mustBe a[JsError]
    }

    "fail to deserialize when countryOfOrigin is invalid" in {
      val json = Json.parse(
        """{
          |  "actorId": "GB123456789012",
          |  "traderRef": "Trader123",
          |  "comcode": "123456",
          |  "goodsDescription": "Electronic goods",
          |  "countryOfOrigin": "INVALID",
          |  "category": 2,
          |  "comcodeEffectiveFromDate": "2024-05-12T12:15:15Z"
          |}""".stripMargin
      )

      val result = json.validate[CreateRecordRequest]
      result mustBe a[JsError]
    }

    "fail to deserialize when category is out of range" in {
      val json = Json.parse(
        """{
          |  "actorId": "GB123456789012",
          |  "traderRef": "Trader123",
          |  "comcode": "123456",
          |  "goodsDescription": "Electronic goods",
          |  "countryOfOrigin": "GB",
          |  "category": 5,
          |  "comcodeEffectiveFromDate": "2024-05-12T12:15:15Z"
          |}""".stripMargin
      )

      val result = json.validate[CreateRecordRequest]
      result mustBe a[JsError]
    }

    "serialize correctly" in {
      val request = CreateRecordRequest(
        actorId = "GB123456789012",
        traderRef = "Trader123",
        comcode = "123456",
        goodsDescription = "Electronic goods",
        countryOfOrigin = "GB",
        category = Some(2),
        assessments = None,
        supplementaryUnit = Some(BigDecimal(10.5)),
        measurementUnit = Some("kg"),
        comcodeEffectiveFromDate = Instant.parse("2024-05-12T12:15:15Z"),
        comcodeEffectiveToDate = Some(Instant.parse("2025-05-12T12:15:15Z"))
      )

      val expectedJson = Json.parse(
        """{
          |  "actorId": "GB123456789012",
          |  "traderRef": "Trader123",
          |  "comcode": "123456",
          |  "goodsDescription": "Electronic goods",
          |  "countryOfOrigin": "GB",
          |  "category": 2,
          |  "supplementaryUnit": 10.5,
          |  "measurementUnit": "kg",
          |  "comcodeEffectiveFromDate": "2024-05-12T12:15:15Z",
          |  "comcodeEffectiveToDate": "2025-05-12T12:15:15Z"
          |}""".stripMargin
      )

      Json.toJson(request) mustBe expectedJson
    }
  }
}
