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

package uk.gov.hmrc.tradergoodsprofilesrouter.service.audit

import org.scalatest.OptionValues
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.AdviceStatus._
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.Pagination
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.{AdviceStatus, GetRecordsResponse, GoodsItemRecords}

import java.time.Instant

class AuditGetRecordResponseMappingSpec extends PlaySpec with OptionValues {

  object MapperTest extends AuditGetRecordResponseMapping

  "collectReviewReasonSummary" should {
    "return the count of record in review" in {
      val reviewReason = Seq("Commodity code changed", "Expired", "Expired", "Expired", "Commodity code changed")

      val result =
        MapperTest.collectReviewReasonSummary(createEisMultipleRecordsResponse(reviewReason).goodsItemRecords)

      result.value mustBe Map("Commodity code changed" -> 2, "Expired" -> 3)
    }

    "return None when reviewReason not available" in {
      val result = MapperTest.collectReviewReasonSummary(createEisMultipleRecordsResponse().goodsItemRecords)

      result mustBe None
    }

    "return None if empty records" in {
      val result = MapperTest.collectReviewReasonSummary(Seq.empty)

      result mustBe None
    }
  }

  "collectAccreditationStatusSummary" should {
    "collect all the accreditationStatus" in {
      val adviceStatus = Seq(NotRequested, AdviceRequestWithdrawn, NotRequested, Requested, AdviceRequestWithdrawn)
      val result       = MapperTest.collectAccreditationStatusSummary(
        createEisMultipleRecordsResponse(adviceStatuses = adviceStatus).goodsItemRecords
      )

      result.value mustBe Map("Not Requested" -> 2, "Advice request withdrawn" -> 2, "Requested" -> 1)
    }

    "return None if empty records" in {
      val result = MapperTest.collectAccreditationStatusSummary(Seq.empty)

      result mustBe None
    }
  }

  "collectCategorySummary" should {
    "return the summary" in {
      val categories = Seq(1, 2, 3, 1, 2)
      val result     = MapperTest.collectCategorySummary(
        createEisMultipleRecordsResponse(categories = categories).goodsItemRecords
      )

      result mustBe Some(Map(1 -> 2, 2 -> 2, 3 -> 1))
    }

    "return none if category is missing" in {
      val result = MapperTest.collectCategorySummary(createEisMultipleRecordsResponse().goodsItemRecords)

      result mustBe None
    }

    "return None if empty records" in {
      val result = MapperTest.collectCategorySummary(Seq.empty)

      result mustBe None
    }
  }

  private def createEisMultipleRecordsResponse(
    reviewReasons: Seq[String] = Seq.empty,
    adviceStatuses: Seq[AdviceStatus] = Seq.empty,
    categories: Seq[Int] = Seq.empty
  ): GetRecordsResponse = {

    val records = for {
      i           <- 0 until 5
      reviewReason = if (i < reviewReasons.size) Some(reviewReasons(i)) else None
      adviceStatus = if (i < adviceStatuses.size) adviceStatuses(i) else NotRequested
      category     = if (i < categories.size) Some(categories(i)) else None
      record       = createEISGetRecordResponseWithVariable(reviewReason, adviceStatus, category)
    } yield record

    GetRecordsResponse(records, Pagination(4, 2, 3, Some(3), Some(1)))

  }

  private def createEISGetRecordResponseWithVariable(
    reviewReason: Option[String],
    adviceStatus: AdviceStatus,
    category: Option[Int]
  ): GoodsItemRecords =
    GoodsItemRecords(
      eori = "GB123456789011",
      actorId = "GB1234567890",
      recordId = "d677693e-9981-4ee3-8574-654981ebe606",
      traderRef = "BAN001001",
      comcode = "10410100",
      adviceStatus = adviceStatus,
      goodsDescription = "Organic bananas",
      countryOfOrigin = "EC",
      category = category,
      assessments = None,
      supplementaryUnit = Some(BigDecimal(500)),
      measurementUnit = Some("square meters(m^2)"),
      comcodeEffectiveFromDate = Instant.now,
      comcodeEffectiveToDate = None,
      version = 1,
      active = true,
      toReview = true,
      reviewReason = reviewReason,
      declarable = "IMMI Ready",
      ukimsNumber = "XIUKIM47699357400020231115081800",
      nirmsNumber = Some("RMS-GB-123456"),
      niphlNumber = Some("--1234"),
      locked = false,
      createdDateTime = Instant.now,
      updatedDateTime = Instant.now
    )
}
