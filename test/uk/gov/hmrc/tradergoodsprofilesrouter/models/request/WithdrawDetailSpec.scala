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

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.withdrawAdvice.withdrawAdvice.WithdrawDetail

import java.time.Instant

class WithdrawDetailSpec extends PlaySpec {

  "WithdrawDetail should trim spaces from withdrawReason" in {
    val jsonWithSpaces = Json.parse(
      """
        |{
        |  "withdrawDate": "2023-10-11T12:00:00Z",
        |  "withdrawReason": "  some reason  "
        |}
      """.stripMargin
    )

    val expectedWithdrawDetail = WithdrawDetail(
      withdrawDate = Instant.parse("2023-10-11T12:00:00Z"),
      withdrawReason = Some("some reason")
    )

    jsonWithSpaces.as[WithdrawDetail] shouldEqual expectedWithdrawDetail
  }

  "WithdrawDetail handle None withdrawReason when not specified" in {
    val jsonWithoutReason = Json.parse(
      """
        |{
        |  "withdrawDate": "2023-10-11T12:00:00Z"
        |}
      """.stripMargin
    )

    val expectedWithdrawDetail = WithdrawDetail(
      withdrawDate = Instant.parse("2023-10-11T12:00:00Z"),
      withdrawReason = None
    )

    jsonWithoutReason.as[WithdrawDetail] shouldEqual expectedWithdrawDetail
  }
}
