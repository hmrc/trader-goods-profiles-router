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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.response

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, Json}

class CorrelationIdSpec extends AnyWordSpec with Matchers {

  "CorrelationId" should {

    val validJson = Json.parse(
      """
        |{
        |  "correlationId": "123e4567-e89b-12d3-a456-426614174000"
        |}
        |""".stripMargin
    )

    val validCorrelationId = CorrelationId("123e4567-e89b-12d3-a456-426614174000")

    "deserialize valid JSON correctly" in {
      val result = validJson.validate[CorrelationId]
      result mustBe JsSuccess(validCorrelationId)
    }

    "fail to deserialize invalid JSON" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "correlation": "missing_field"
          |}
          |""".stripMargin
      )

      val result = invalidJson.validate[CorrelationId]
      result mustBe a[JsError]
    }

    "serialize correctly" in {
      val result = Json.toJson(validCorrelationId)
      result mustBe validJson
    }
  }
}
