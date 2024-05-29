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
import play.api.libs.json.Json

class CreateRecordRequestSpec extends PlaySpec {

  "CreateRecordRequest validation" should {

    "validate category within range 1 to 3" in {
      val validJson1 = Json.parse("""
          |{
          |  "eori": "GB123456789012",
          |  "actorId": "GB987654321098",
          |  "traderRef": "SKU123456",
          |  "comcode": "123456",
          |  "goodsDescription": "Bananas",
          |  "countryOfOrigin": "GB",
          |  "category": 1,
          |  "comcodeEffectiveFromDate": "2023-01-01T00:00:00Z"
          |}
        """.stripMargin)

      val validJson2 = Json.parse("""
          |{
          |  "eori": "GB123456789012",
          |  "actorId": "GB987654321098",
          |  "traderRef": "SKU123456",
          |  "comcode": "123456",
          |  "goodsDescription": "Bananas",
          |  "countryOfOrigin": "GB",
          |  "category": 2,
          |  "comcodeEffectiveFromDate": "2023-01-01T00:00:00Z"
          |}
        """.stripMargin)

      val validJson3 = Json.parse("""
          |{
          |  "eori": "GB123456789012",
          |  "actorId": "GB987654321098",
          |  "traderRef": "SKU123456",
          |  "comcode": "123456",
          |  "goodsDescription": "Bananas",
          |  "countryOfOrigin": "GB",
          |  "category": 3,
          |  "comcodeEffectiveFromDate": "2023-01-01T00:00:00Z"
          |}
        """.stripMargin)

      val invalidJson = Json.parse("""
          |{
          |  "eori": "GB123456789012",
          |  "actorId": "GB987654321098",
          |  "traderRef": "SKU123456",
          |  "comcode": "123456",
          |  "goodsDescription": "Bananas",
          |  "countryOfOrigin": "GB",
          |  "category": 24,
          |  "comcodeEffectiveFromDate": "2023-01-01T00:00:00Z"
          |}
        """.stripMargin)

      validJson1.validate[CreateRecordRequest].isSuccess mustBe true
      validJson2.validate[CreateRecordRequest].isSuccess mustBe true
      validJson3.validate[CreateRecordRequest].isSuccess mustBe true
      invalidJson.validate[CreateRecordRequest].isError mustBe true
    }
  }
}
