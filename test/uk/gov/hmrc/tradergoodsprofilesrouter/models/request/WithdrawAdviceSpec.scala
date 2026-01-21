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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.withdrawAdvice

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.withdrawAdvice.withdrawAdvice.*

import java.time.Instant

class WithdrawAdviceSpec extends PlaySpec {

  "RequestCommon" should {
    "serialize and deserialize correctly" in {
      val model = RequestCommon(Some("client-123"))
      val json  = Json.toJson(model)

      json mustBe Json.parse("""{"clientID": "client-123"}""")
      json.validate[RequestCommon] mustBe JsSuccess(model)
    }

    "handle missing clientID" in {
      val json = Json.parse("""{}""")
      json.validate[RequestCommon] mustBe JsSuccess(RequestCommon(None))
    }
  }

  "WithdrawDetail" should {
    "serialize and deserialize correctly" in {
      val instant = Instant.parse("2024-02-17T12:00:00Z")
      val model   = WithdrawDetail(instant, Some("Reason for withdrawal"))
      val json    = Json.toJson(model)

      json mustBe Json.parse("""{"withdrawDate": "2024-02-17T12:00:00Z", "withdrawReason": "Reason for withdrawal"}""")
      json.validate[WithdrawDetail] mustBe JsSuccess(model)
    }

    "handle missing withdrawReason" in {
      val instant = Instant.parse("2024-02-17T12:00:00Z")
      val json    = Json.parse("""{"withdrawDate": "2024-02-17T12:00:00Z"}""")

      json.validate[WithdrawDetail] mustBe JsSuccess(WithdrawDetail(instant, None))
    }
  }

  "PublicRecordID" should {
    "serialize and deserialize correctly" in {
      val model = PublicRecordID("PR123")
      val json  = Json.toJson(model)

      json mustBe Json.parse("""{"publicRecordID": "PR123"}""")
      json.validate[PublicRecordID] mustBe JsSuccess(model)
    }
  }

  "RequestDetail" should {
    "serialize and deserialize correctly" in {
      val instant        = Instant.parse("2024-02-17T12:00:00Z")
      val withdrawDetail = WithdrawDetail(instant, Some("Valid reason"))
      val publicRecord   = PublicRecordID("PR123")
      val model          = RequestDetail(withdrawDetail, Seq(publicRecord))
      val json           = Json.toJson(model)

      json mustBe Json.parse(
        """{
          |"withdrawDetail": {"withdrawDate": "2024-02-17T12:00:00Z", "withdrawReason": "Valid reason"},
          |"goodsItems": [{"publicRecordID": "PR123"}]
          |}""".stripMargin
      )

      json.validate[RequestDetail] mustBe JsSuccess(model)
    }
  }

  "WithdrawRequest" should {
    "serialize and deserialize correctly" in {
      val instant        = Instant.parse("2024-02-17T12:00:00Z")
      val withdrawDetail = WithdrawDetail(instant, Some("Valid reason"))
      val publicRecord   = PublicRecordID("PR123")
      val requestDetail  = RequestDetail(withdrawDetail, Seq(publicRecord))
      val model          = WithdrawRequest(Some(RequestCommon(Some("client-123"))), requestDetail)
      val json           = Json.toJson(model)

      json mustBe Json.parse(
        """{
          |"requestCommon": {"clientID": "client-123"},
          |"requestDetail": {
          |  "withdrawDetail": {"withdrawDate": "2024-02-17T12:00:00Z", "withdrawReason": "Valid reason"},
          |  "goodsItems": [{"publicRecordID": "PR123"}]
          |}
          |}""".stripMargin
      )

      json.validate[WithdrawRequest] mustBe JsSuccess(model)
    }
  }

  "WithdrawAdvicePayload" should {
    "serialize and deserialize correctly" in {
      val instant         = Instant.parse("2024-02-17T12:00:00Z")
      val withdrawDetail  = WithdrawDetail(instant, Some("Valid reason"))
      val publicRecord    = PublicRecordID("PR123")
      val requestDetail   = RequestDetail(withdrawDetail, Seq(publicRecord))
      val withdrawRequest = WithdrawRequest(Some(RequestCommon(Some("client-123"))), requestDetail)
      val model           = WithdrawAdvicePayload(withdrawRequest)
      val json            = Json.toJson(model)

      json mustBe Json.parse(
        """{
          |"withdrawRequest": {
          |  "requestCommon": {"clientID": "client-123"},
          |  "requestDetail": {
          |    "withdrawDetail": {"withdrawDate": "2024-02-17T12:00:00Z", "withdrawReason": "Valid reason"},
          |    "goodsItems": [{"publicRecordID": "PR123"}]
          |  }
          |}
          |}""".stripMargin
      )

      json.validate[WithdrawAdvicePayload] mustBe JsSuccess(model)
    }
  }
}
