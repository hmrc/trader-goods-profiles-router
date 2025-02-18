/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.response

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.GetRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{EisGoodsItemRecords, Pagination}
import java.time.Instant
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.GoodsItemRecords
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.AccreditationStatus

class AuditGetRecordsResponseSpec extends AnyWordSpec with Matchers {

  "AuditGetRecordsResponse" should {

    val pagination = Pagination(
      totalRecords = 2,
      currentPage = 1,
      totalPages = 1,
      nextPage = None,
      prevPage = None
    )

    val goodsItemRecords = Seq(
      EisGoodsItemRecords(
        eori = "GB123456789012",
        actorId = "GB987654321098",
        recordId = "RID12345",
        traderRef = "TREF123",
        comcode = "123456",
        accreditationStatus = AccreditationStatus.Approved,
        goodsDescription = "Goods Desc",
        countryOfOrigin = "GB",
        category = Some(1),
        assessments = None,
        supplementaryUnit = Some(10),
        measurementUnit = Some("kg"),
        comcodeEffectiveFromDate = Instant.now(),
        comcodeEffectiveToDate = None,
        version = 1,
        active = true,
        toReview = false,
        reviewReason = None,
        declarable = "IMMI Ready",
        ukimsNumber = "UKIMS123",
        nirmsNumber = Some("NIRMS456"),
        niphlNumber = Some("NIPHL789"),
        locked = false,
        createdDateTime = Instant.now(),
        updatedDateTime = Instant.now()
      )
    )

    val getRecordsResponse = GetRecordsResponse(
      goodsItemRecords = goodsItemRecords.map(GoodsItemRecords(_)), 
      pagination = pagination
    )


    "correctly map from GetRecordsResponse" in {
      val result = AuditGetRecordsResponse(getRecordsResponse)

      result.totalRecords mustBe 2
      result.payloadRecords mustBe 1
      result.currentPage mustBe 1
      result.totalPages mustBe 1
      result.nextPage mustBe None
      result.prevPage mustBe None
      result.IMMIReadyCount mustBe Some(1)
      result.notIMMIReadyCount mustBe Some(0)
      result.notReadyForUseCount mustBe Some(0)
      result.forReviewCount mustBe Some(0)
      result.reviewReasons mustBe None
      result.lockedCount mustBe Some(0)
      result.activeCount mustBe Some(1)
      result.adviceStatuses mustBe Some(Map("Advice Provided" -> 1))
      result.categories mustBe Some(Map(1 -> 1))
      result.UKIMSNumber mustBe Some("UKIMS123")
      result.NIRMSNumber mustBe Some("NIRMS456")
      result.NIPHLNumber mustBe Some("NIPHL789") 
    }

    "serialize correctly" in {
      val json = Json.toJson(AuditGetRecordsResponse(getRecordsResponse))
      json.validate[AuditGetRecordsResponse] mustBe a[JsSuccess[_]]
    }
  }
}
