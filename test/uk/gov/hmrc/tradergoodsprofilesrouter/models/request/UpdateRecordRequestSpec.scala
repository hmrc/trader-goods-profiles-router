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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.Assessment

import java.time.Instant

class UpdateRecordRequestSpec extends AnyWordSpec with Matchers {

  "UpdateRecordRequest JSON format" should {

    "successfully deserialize valid JSON" in {
      val json = Json.parse(
        """{
          |  "actorId": "GB987654321098",
          |  "traderRef": "TR123",
          |  "comcode": "12345678",
          |  "goodsDescription": "Example goods",
          |  "countryOfOrigin": "GB",
          |  "category": 2,
          |  "assessments": [],
          |  "supplementaryUnit": 10.5,
          |  "measurementUnit": "kg",
          |  "comcodeEffectiveFromDate": "2024-01-01T12:00:00Z",
          |  "comcodeEffectiveToDate": "2024-12-31T12:00:00Z"
          |}""".stripMargin)

      val result = json.validate[UpdateRecordRequest]

      result shouldBe a[JsSuccess[_]]
    }

    "fail to deserialize invalid actorId" in {
      val invalidJson = Json.parse(
        """{
          |  "actorId": "123",
          |  "traderRef": "TR123",
          |  "comcode": "12345678",
          |  "goodsDescription": "Example goods",
          |  "countryOfOrigin": "GB",
          |  "comcodeEffectiveFromDate": "2024-01-01T12:00:00Z"
          |}""".stripMargin)

      val result = invalidJson.validate[UpdateRecordRequest]

      result shouldBe a[JsError]
    }

    "fail to deserialize invalid countryOfOrigin" in {
      val invalidJson = Json.parse(
        """{
          |  "actorId": "GB987654321098",
          |  "traderRef": "TR123",
          |  "comcode": "12345678",
          |  "goodsDescription": "Example goods",
          |  "countryOfOrigin": "XYZ",
          |  "comcodeEffectiveFromDate": "2024-01-01T12:00:00Z"
          |}""".stripMargin)

      val result = invalidJson.validate[UpdateRecordRequest]

      result shouldBe a[JsError]
    }

    "serialize to JSON correctly" in {
      val request = UpdateRecordRequest(
        actorId = "GB987654321098",
        traderRef = "TR123",
        comcode = "12345678",
        goodsDescription = "Example goods",
        countryOfOrigin = "GB",
        category = Some(2),
        assessments = Some(Seq.empty[Assessment]),
        supplementaryUnit = Some(BigDecimal(10.5)),
        measurementUnit = Some("kg"),
        comcodeEffectiveFromDate = Instant.parse("2024-01-01T12:00:00Z"),
        comcodeEffectiveToDate = Some(Instant.parse("2024-12-31T12:00:00Z"))
      )

      val expectedJson = Json.parse(
        """{
          |  "actorId": "GB987654321098",
          |  "traderRef": "TR123",
          |  "comcode": "12345678",
          |  "goodsDescription": "Example goods",
          |  "countryOfOrigin": "GB",
          |  "category": 2,
          |  "assessments": [],
          |  "supplementaryUnit": 10.5,
          |  "measurementUnit": "kg",
          |  "comcodeEffectiveFromDate": "2024-01-01T12:00:00Z",
          |  "comcodeEffectiveToDate": "2024-12-31T12:00:00Z"
          |}""".stripMargin)

      Json.toJson(request) shouldBe expectedJson
    }

    "serialize correctly when optional fields are None" in {
      val request = UpdateRecordRequest(
        actorId = "GB987654321098",
        traderRef = "TR123",
        comcode = "12345678",
        goodsDescription = "Example goods",
        countryOfOrigin = "GB",
        category = None,
        assessments = None,
        supplementaryUnit = None,
        measurementUnit = None,
        comcodeEffectiveFromDate = Instant.parse("2024-01-01T12:00:00Z"),
        comcodeEffectiveToDate = None
      )

      val expectedJson = Json.parse(
        """{
          |  "actorId": "GB987654321098",
          |  "traderRef": "TR123",
          |  "comcode": "12345678",
          |  "goodsDescription": "Example goods",
          |  "countryOfOrigin": "GB",
          |  "comcodeEffectiveFromDate": "2024-01-01T12:00:00Z"
          |}""".stripMargin) // Optional fields should not appear in JSON

      Json.toJson(request) shouldBe expectedJson
    }
  }
}

