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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.audit

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, Json}

class AuditOutcomeSpec extends AnyWordSpec with Matchers {

  "AuditOutcome" should {

    val validJson = Json.parse(
      """
        |{
        |  "status": "Success",
        |  "statusCode": 200,
        |  "failureReason": ["Invalid request", "Missing fields"]
        |}
        |""".stripMargin
    )

    val validAuditOutcome = AuditOutcome(
      status = "Success",
      statusCode = 200,
      failureReason = Some(Seq("Invalid request", "Missing fields"))
    )

    "deserialize valid JSON correctly" in {
      val result = validJson.validate[AuditOutcome]
      result mustBe JsSuccess(validAuditOutcome)
    }

    "fail to deserialize invalid JSON" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "statusCode": "Not a number"
          |}
          |""".stripMargin
      )

      val result = invalidJson.validate[AuditOutcome]
      result mustBe a[JsError]
    }

    "serialize correctly" in {
      val result = Json.toJson(validAuditOutcome)
      result mustBe validJson
    }

    "serialize without failureReason when None" in {
      val auditWithoutFailureReason = AuditOutcome(
        status = "Failure",
        statusCode = 500,
        failureReason = None
      )

      val expectedJson = Json.parse(
        """
          |{
          |  "status": "Failure",
          |  "statusCode": 500
          |}
          |""".stripMargin
      )

      val result = Json.toJson(auditWithoutFailureReason)
      result mustBe expectedJson
    }
  }
}
