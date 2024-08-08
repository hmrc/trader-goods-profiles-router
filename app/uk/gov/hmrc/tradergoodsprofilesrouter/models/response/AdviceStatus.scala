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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.response

import enumeratum.{EnumEntry, PlayEnum}

sealed abstract class AdviceStatus(override val entryName: String) extends EnumEntry

object AdviceStatus extends PlayEnum[AdviceStatus] {

  val AllowedAdviceStatuses: Seq[AdviceStatus] = Seq(NotRequested, AdviceRequestWithdrawn, AdviceNotProvided)

  override val values: IndexedSeq[AdviceStatus] = findValues

  case object NotRequested extends AdviceStatus("Not Requested")
  case object Requested extends AdviceStatus("Requested")
  case object InProgress extends AdviceStatus("In progress")
  case object InformationRequested extends AdviceStatus("Information Requested")
  case object AdviceRequestWithdrawn extends AdviceStatus("Advice request withdrawn")
  case object AdviceProvided extends AdviceStatus("Advice Provided")
  case object AdviceNotProvided extends AdviceStatus("Advice not provided")
}
