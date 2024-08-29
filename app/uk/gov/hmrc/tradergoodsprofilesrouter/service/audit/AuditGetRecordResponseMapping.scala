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

import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.EisGoodsItemRecords

trait AuditGetRecordResponseMapping {

  def filterIMMReadyCount(declarable: Seq[String]): Int =
    declarable.filter(_.equalsIgnoreCase("IMMI Ready")).length

  def filterNotIMMReadyCount(declarable: Seq[String]): Int =
    declarable.filter(_.equalsIgnoreCase("Not Ready for IMMI")).length

  def filterNotReadyForUseCount(declarable: Seq[String]): Int =
    declarable.filter(_.equalsIgnoreCase("Not Ready for Use")).length

  def collectReviewReasonSummary(records: Seq[EisGoodsItemRecords]): Option[Map[String,Int]] = {

    val viewReason =
      records
        .groupBy(_.reviewReason)
        .collect{ case (Some(k),v) => k -> v.length}

    if(viewReason.nonEmpty) Some(viewReason) else None
  }

  def collectAccreditationStatusSummary(records: Seq[EisGoodsItemRecords]): Map[String, Int] =
    records
      .groupBy(_.accreditationStatus)
      .collect{ case (k,v) => k.entryName -> v.length }

  def collectCategorySummary(records: Seq[EisGoodsItemRecords]): Option[Map[Int, Int]] = {
    val categorySummary = records
      .groupBy(_.category)
      .collect{ case (Some(k),v) => k -> v.length }

    if(categorySummary.nonEmpty) Some(categorySummary) else None
  }
}
