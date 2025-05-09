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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.advicerequests

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, Json}

class GoodsItemSpec extends AnyWordSpec with Matchers {

  "GoodsItem" should {

    val validJson = Json.parse(
      """
        |{
        |  "publicRecordID": "PR12345",
        |  "traderReference": "TR98765",
        |  "goodsDescription": "Electronic Goods",
        |  "countryOfOrigin": "GB",
        |  "supplementaryUnit": 10.5,
        |  "category": 3,
        |  "measurementUnitDescription": "kg",
        |  "commodityCode": "12345678"
        |}
        |""".stripMargin
    )

    val validGoodsItem = GoodsItem(
      publicRecordID = "PR12345",
      traderReference = "TR98765",
      goodsDescription = "Electronic Goods",
      countryOfOrigin = Some("GB"),
      supplementaryUnit = Some(BigDecimal(10.5)),
      category = Some(3),
      measurementUnitDescription = Some("kg"),
      commodityCode = "12345678"
    )

    "deserialize valid JSON correctly" in {
      val result = validJson.validate[GoodsItem]
      result mustBe JsSuccess(validGoodsItem)
    }

    "fail to deserialize invalid JSON" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "publicRecordID": "PR12345",
          |  "traderReference": "TR98765"
          |}
          |""".stripMargin
      )

      val result = invalidJson.validate[GoodsItem]
      result mustBe a[JsError]
    }

    "serialize correctly" in {
      val result = Json.toJson(validGoodsItem)
      result mustBe validJson
    }

    "serialize without optional fields when they are None" in {
      val goodsItemWithoutOptionalFields = GoodsItem(
        publicRecordID = "PR12345",
        traderReference = "TR98765",
        goodsDescription = "Electronic Goods",
        countryOfOrigin = None,
        supplementaryUnit = None,
        category = None,
        measurementUnitDescription = None,
        commodityCode = "12345678"
      )

      val expectedJson = Json.parse(
        """
          |{
          |  "publicRecordID": "PR12345",
          |  "traderReference": "TR98765",
          |  "goodsDescription": "Electronic Goods",
          |  "commodityCode": "12345678"
          |}
          |""".stripMargin
      )

      val result = Json.toJson(goodsItemWithoutOptionalFields)
      result mustBe expectedJson
    }
  }
}
