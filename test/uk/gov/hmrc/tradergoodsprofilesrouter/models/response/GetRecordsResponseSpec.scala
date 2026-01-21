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

import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{AccreditationStatus, EisGoodsItemRecords, Pagination}

import java.time.Instant

class GetRecordsResponseSpec extends AnyWordSpec with Matchers {

  "GetRecordsResponse" should {

    "serialize correctly" in {
      val response = GetRecordsResponse(
        goodsItemRecords = Seq(
          GoodsItemRecords(
            EisGoodsItemRecords(
              eori = "GB123456789",
              actorId = "ACTOR123456",
              recordId = "RECORD001",
              traderRef = "TRADER001",
              comcode = "12345678",
              accreditationStatus = AccreditationStatus.Approved,
              goodsDescription = "Example Goods",
              countryOfOrigin = "GB",
              category = Some(2),
              assessments = None,
              supplementaryUnit = Some(10.5),
              measurementUnit = Some("kg"),
              comcodeEffectiveFromDate = Instant.parse("2024-01-01T12:00:00Z"),
              comcodeEffectiveToDate = Some(Instant.parse("2024-12-31T12:00:00Z")),
              version = 1,
              active = true,
              toReview = false,
              reviewReason = None,
              declarable = "YES",
              ukimsNumber = "UK123456",
              nirmsNumber = Some("NI789456"),
              niphlNumber = None,
              locked = false,
              createdDateTime = Instant.parse("2024-01-01T12:00:00Z"),
              updatedDateTime = Instant.parse("2024-01-02T12:00:00Z")
            )
          )
        ),
        pagination = Pagination(1, 10, 2, Some(2), Some(1))
      )

      val json = Json.toJson(response)
      json.validate[GetRecordsResponse] mustBe a[JsSuccess[_]]
    }

    "deserialize valid JSON correctly" in {
      val json = Json.parse(
        """{
          |  "goodsItemRecords": [],
          |  "pagination": { "totalRecords": 1, "currentPage": 1, "totalPages": 1, "nextPage": null, "prevPage": null }
          |}""".stripMargin
      )

      json.validate[GetRecordsResponse] mustBe a[JsSuccess[_]]
    }

    "fail to deserialize invalid JSON" in {
      val invalidJson = Json.parse("""{ "invalidField": "some value" }""")
      invalidJson.validate[GetRecordsResponse] mustBe a[JsError]
    }
  }
}
