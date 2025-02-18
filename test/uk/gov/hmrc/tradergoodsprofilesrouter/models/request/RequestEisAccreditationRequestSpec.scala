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

class RequestEisAccreditationRequestSpec extends AnyWordSpec with Matchers {

  "RequestEisAccreditationRequest" should {

    val validTraderDetails = TraderDetails(
      traderEORI = "GB123456789012",
      requestorName = "John Doe",
      requestorEORI = Some("GB987654321098"),
      requestorEmail = "john.doe@example.com",
      ukimsAuthorisation = "Authorized",
      goodsItems = Seq()
    )

    val validJson = Json.parse(
      """
        |{
        |  "accreditationRequest": {
        |    "requestCommon": {
        |      "receiptDate": "2025-02-17T12:00:00Z"
        |    },
        |    "requestDetail": {
        |      "traderDetails": {
        |        "traderEORI": "GB123456789012",
        |        "requestorName": "John Doe",
        |        "requestorEORI": "GB987654321098",
        |        "requestorEmail": "john.doe@example.com",
        |        "ukimsAuthorisation": "Authorized",
        |        "goodsItems": []
        |      }
        |    }
        |  }
        |}
        |""".stripMargin
    )

    val validRequest = RequestEisAccreditationRequest(
      AccreditationRequest(
        requestCommon = RequestCommon(clientID = None, receiptDate = "2025-02-17T12:00:00Z", boxID = None),
        requestDetail = RequestDetail(traderDetails = validTraderDetails)
      )
    )

    "deserialize valid JSON correctly" in {
      val result = validJson.validate[RequestEisAccreditationRequest]
      result mustBe JsSuccess(validRequest)
    }

    "fail to deserialize invalid JSON" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "accreditationRequest": {
          |    "requestCommon": {
          |      "receiptDate": ""
          |    },
          |    "requestDetail": {}
          |  }
          |}
          |""".stripMargin
      )

      val result = invalidJson.validate[RequestEisAccreditationRequest]
      result mustBe a[JsError]
    }

    "serialize correctly" in {
      val result = Json.toJson(validRequest)

      val expectedJson = Json.parse(
        """
          |{
          |  "accreditationRequest": {
          |    "requestCommon": {
          |      "receiptDate": "2025-02-17T12:00:00Z"
          |    },
          |    "requestDetail": {
          |      "traderDetails": {
          |        "traderEORI": "GB123456789012",
          |        "requestorName": "John Doe",
          |        "requestorEORI": "GB987654321098",
          |        "requestorEmail": "john.doe@example.com",
          |        "ukimsAuthorisation": "Authorized",
          |        "goodsItems": []
          |      }
          |    }
          |  }
          |}
          |""".stripMargin
      )

      result mustBe expectedJson
    }

    "create an instance using the apply method" in {
      val result = RequestEisAccreditationRequest(
        traderDetails = validTraderDetails,
        dateTime = "2025-02-17T12:00:00Z"
      )

      result mustBe validRequest
    }
  }
}
