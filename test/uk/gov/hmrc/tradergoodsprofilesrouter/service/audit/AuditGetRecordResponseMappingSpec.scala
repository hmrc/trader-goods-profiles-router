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

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.AccreditationStatus.{NotRequested, Requested, Withdrawn}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{AccreditationStatus, EisGoodsItemRecords, GetEisRecordsResponse, Pagination}

import java.time.Instant

class AuditGetRecordResponseMappingSpec extends PlaySpec {

  object MapperTest extends AuditGetRecordResponseMapping

  "filterIMMReadyCount" should {
    "return a Map" in {

      val declarable = Seq(
        "IMMI Ready", "Not Ready for IMMI", "IMMI Ready", "IMMI Ready", "Not Ready for IMMI"
      )
      MapperTest.filterIMMReadyCount(declarable) mustBe 3
    }

    "return 0 if not found" in {
      MapperTest.filterIMMReadyCount(Seq()) mustBe 0
    }

    "ignore cases" in {
      val declarable = Seq(
        "ImmI ready", "Not Ready for IMMI", "immi ready", "IMMI Ready", "Not Ready for IMMI"
      )
      MapperTest.filterIMMReadyCount(declarable) mustBe 3
    }
  }

  "filterNotIMMReadyCount" should {
    "return a Map" in {
      val declarable = Seq(
        "IMMI Ready", "Not Ready for IMMI", "IMMI Ready", "IMMI Ready", "Not Ready for IMMI"
      )
      MapperTest.filterNotIMMReadyCount(declarable) mustBe 2
    }

    "return 0 if not found" in {
      MapperTest.filterNotIMMReadyCount(Seq()) mustBe 0

    }

    "ignore cases" in {
      val declarable = Seq(
        "IMMI Ready", "not ready for immi", "IMMI Ready", "IMMI Ready", "Not Ready for IMMI"
      )
      MapperTest.filterNotIMMReadyCount(declarable) mustBe 2
    }
  }

  "filterNotReadyForUseCount" should {
    "return a Map" in {
      val declarable = Seq(
        "IMMI Ready", "Not Ready for Use", "IMMI Ready", "IMMI Ready", "Not Ready for IMMI"
      )
      MapperTest.filterNotReadyForUseCount(declarable) mustBe 1
    }

    "return 0 if not found" in {
      MapperTest.filterNotReadyForUseCount(Seq()) mustBe 0

    }

    "ignore cases" in {
      val declarable = Seq(
        "IMMI Ready", "not ready for use", "IMMI Ready", "IMMI Ready", "Not Ready for IMMI"
      )
      MapperTest.filterNotReadyForUseCount(declarable) mustBe 1
    }
  }

  "collectReviewReasonSummary" should {
    "return the count of record in review" in {
      val reviewReason = Seq("Commodity code changed", "Expired", "Expired", "Expired", "Commodity code changed")

      val result = MapperTest.collectReviewReasonSummary(
        createEisMultipleRecordsResponse(reviewReason).goodsItemRecords)

      result mustBe Some(Map("Commodity code changed" -> 2, "Expired" -> 3))
    }

    "return None when reviewReason not available" in {
      val result = MapperTest.collectReviewReasonSummary(createEisMultipleRecordsResponse().goodsItemRecords)

      result mustBe None
    }
  }

  "collectAccreditationStatusSummary" should {
    "collect all the accreditationStatus" in {

      val accreditationStatus = Seq(NotRequested, Withdrawn, NotRequested, Requested, Withdrawn)
      val result = MapperTest.collectAccreditationStatusSummary(createEisMultipleRecordsResponse(accreditationStatuses = accreditationStatus).goodsItemRecords)

      result mustBe Map("Not Requested" -> 2, "Withdrawn" -> 2, "Requested" -> 1)
    }
  }

  "collectCategorySummary" should {
    "return the summary" in {
      val categories = Seq(1, 2, 3, 1, 2)
      val result = MapperTest.collectCategorySummary(
        createEisMultipleRecordsResponse(categories = categories).goodsItemRecords
      )

      result mustBe Some(Map(1 -> 2, 2 -> 2, 3 -> 1))
    }

    "return none if category is missing" in {
      val result = MapperTest.collectCategorySummary(createEisMultipleRecordsResponse().goodsItemRecords)

      result mustBe None
    }
  }

  private def createEisMultipleRecordsResponse
  (
    reviewReasons: Seq[String] = Seq.empty,
    accreditationStatuses: Seq[AccreditationStatus] = Seq.empty,
    categories: Seq[Int] = Seq.empty
  ): GetEisRecordsResponse = {


    val records = for {
      i <- 0 until 5
      reviewReason = if (i < reviewReasons.size) Some(reviewReasons(i)) else None
      accreditationStatus = if (i < accreditationStatuses.size) accreditationStatuses(i) else NotRequested
      category = if(i < categories.size) Some(categories(i)) else None
      record = createEISGetRecordResponseWithVariable(reviewReason, accreditationStatus, category)
    } yield record

    GetEisRecordsResponse(records, Pagination(4, 2, 3, Some(3), Some(1)))

  }

  private def createEISGetRecordResponseWithVariable(
    reviewReason: Option[String],
    accreditationStatus: AccreditationStatus,
    category: Option[Int]
  ): EisGoodsItemRecords = {
    EisGoodsItemRecords(
      eori = "GB123456789011",
      actorId = "GB1234567890",
      recordId = "d677693e-9981-4ee3-8574-654981ebe606",
      traderRef = "BAN001001",
      comcode = "10410100",
      accreditationStatus = accreditationStatus,
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
}
