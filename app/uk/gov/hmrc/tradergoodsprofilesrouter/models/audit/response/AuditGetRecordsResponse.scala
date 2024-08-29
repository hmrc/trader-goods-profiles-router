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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.audit.response

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.audit.AuditGetRecordResponseMapping


case class AuditGetRecordsResponse
(
  totalRecords: Int,
  payloadRecords: Int,
  currentPage: Int,
  totalPages: Int,
  nextPage: Option[Int], //todo: can this be omitted? This is optional on pagination
  prevPage: Option[Int], //todo: can this be omitted? This is optional on pagination
  IMMIReadyCount: Int,
  notIMMIReadyCount: Int,
  notReadyForUseCount: Int,
  forReviewCount: Int,
  reviewReasons: Option[Map[String, Int]],
  lockedCount: Int,
  activeCount: Int,
  adviceStatuses: Map[String, Int],
  categories: Option[Map[Int, Int]],
  UKIMSNumber: String,
  NIRMSNumber: String,
  NIPHLNumber: String
)

object AuditGetRecordsResponse extends AuditGetRecordResponseMapping {
  implicit val format: OFormat[AuditGetRecordsResponse] = Json.format[AuditGetRecordsResponse]

  def apply(response: GetEisRecordsResponse): AuditGetRecordsResponse = {
    AuditGetRecordsResponse(
      totalRecords = response.pagination.totalRecords,
      payloadRecords = response.goodsItemRecords.length,
      currentPage = response.pagination.currentPage,
      totalPages = response.pagination.totalPages,
      nextPage = response.pagination.nextPage,
      prevPage = response.pagination.prevPage,
      IMMIReadyCount = response.goodsItemRecords.filter(
        _.declarable.equalsIgnoreCase("IMMI Ready")
        ).length,
      notIMMIReadyCount = response.goodsItemRecords.filter(
        _.declarable.equalsIgnoreCase("Not Ready for IMMI")
      ).length,
      notReadyForUseCount = response.goodsItemRecords.filter(
        _.declarable.equalsIgnoreCase("Not Ready for Use")
      ).length,
      forReviewCount = response.goodsItemRecords.filter(_.toReview).length,
      reviewReasons = collectReviewReasonSummary(response.goodsItemRecords),
      lockedCount = response.goodsItemRecords.filter(_.locked).length,
      activeCount = response.goodsItemRecords.filter(_.active).length,
      adviceStatuses = collectAccreditationStatusSummary(response.goodsItemRecords),
      categories = collectCategorySummary(response.goodsItemRecords),
      UKIMSNumber = response.goodsItemRecords.headOption.fold[String]("")(r => r.ukimsNumber),
      NIRMSNumber = response.goodsItemRecords.headOption.fold[String]("")(record =>
        record.nirmsNumber.getOrElse("")
      ),
      NIPHLNumber = response.goodsItemRecords.headOption.fold[String]("")(record =>
        record.niphlNumber.getOrElse("")
      )
    )
  }
}
