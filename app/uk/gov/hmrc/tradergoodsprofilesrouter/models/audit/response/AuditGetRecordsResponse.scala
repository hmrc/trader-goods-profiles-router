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
import uk.gov.hmrc.tradergoodsprofilesrouter.models.filters.NiphlNumberFilter
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.GetRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.audit.AuditGetRecordResponseMapping

case class AuditGetRecordsResponse(
  totalRecords: Int,
  payloadRecords: Int,
  currentPage: Int,
  totalPages: Int,
  nextPage: Option[Int],
  prevPage: Option[Int],
  IMMIReadyCount: Option[Int] = None,
  notIMMIReadyCount: Option[Int] = None,
  notReadyForUseCount: Option[Int] = None,
  forReviewCount: Option[Int] = None,
  reviewReasons: Option[Map[String, Int]] = None,
  lockedCount: Option[Int] = None,
  activeCount: Option[Int] = None,
  adviceStatuses: Option[Map[String, Int]] = None,
  categories: Option[Map[Int, Int]] = None,
  UKIMSNumber: Option[String] = None,
  NIRMSNumber: Option[String] = None,
  NIPHLNumber: Option[String] = None
)

object AuditGetRecordsResponse extends AuditGetRecordResponseMapping with NiphlNumberFilter {
  implicit val format: OFormat[AuditGetRecordsResponse] = Json.format[AuditGetRecordsResponse]

  def apply(response: GetRecordsResponse): AuditGetRecordsResponse = {

    val auditResponse = AuditGetRecordsResponse(
      totalRecords = response.pagination.totalRecords,
      payloadRecords = response.goodsItemRecords.length,
      currentPage = response.pagination.currentPage,
      totalPages = response.pagination.totalPages,
      nextPage = response.pagination.nextPage,
      prevPage = response.pagination.prevPage
    )

    response.goodsItemRecords match {
      case Nil     => auditResponse
      case records =>
        auditResponse.copy(
          IMMIReadyCount = Some(records.filter(_.declarable.equalsIgnoreCase("IMMI Ready")).length),
          notIMMIReadyCount = Some(records.filter(_.declarable.equalsIgnoreCase("Not Ready for IMMI")).length),
          notReadyForUseCount = Some(records.filter(_.declarable.equalsIgnoreCase("Not Ready for Use")).length),
          forReviewCount = Some(records.filter(_.toReview).length),
          reviewReasons = collectReviewReasonSummary(records),
          lockedCount = Some(records.filter(_.locked).length),
          activeCount = Some(records.filter(_.active).length),
          adviceStatuses = collectAccreditationStatusSummary(records),
          categories = collectCategorySummary(records),
          UKIMSNumber = response.goodsItemRecords.headOption.map(_.ukimsNumber),
          NIRMSNumber = response.goodsItemRecords.headOption.map(_.nirmsNumber).flatten,
          NIPHLNumber = response.goodsItemRecords.headOption.map(r => removeLeadingDashes(r.niphlNumber)).flatten
        )
    }
  }
}
