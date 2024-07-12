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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.withdrawAdvice

import play.api.libs.json.{JsPath, Json, OFormat, Reads, Writes}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat

import java.time.Instant

object withdrawAdvice {

  case class RequestCommon(clientID: Option[String])

  object RequestCommon {
    implicit val format: OFormat[RequestCommon] = Json.format[RequestCommon]
  }

  case class WithdrawDetail(withdrawDate: Instant, withdrawReason: Option[String])

  object WithdrawDetail {
    import play.api.libs.functional.syntax._

    implicit val format: Reads[WithdrawDetail] = Json.reads[WithdrawDetail]

    implicit val write: Writes[WithdrawDetail] = (
      (JsPath \ "withdrawDate").write[String] and
        (JsPath \ "withdrawReason").writeNullable[String]
      )(e => (e.withdrawDate.asStringSeconds, e.withdrawReason))
  }

  case class PublicRecordID(publicRecordID: String)

  object PublicRecordID {
    implicit val format: OFormat[PublicRecordID] = Json.format[PublicRecordID]
  }

  case class RequestDetail(withdrawDetail: WithdrawDetail, goodsItems: Seq[PublicRecordID])

  object RequestDetail {
    implicit val format: OFormat[RequestDetail] = Json.format[RequestDetail]
  }

  case class WithdrawRequest(requestCommon: Option[RequestCommon], requestDetail: RequestDetail)

  object WithdrawRequest {
    implicit val format: OFormat[WithdrawRequest] = Json.format[WithdrawRequest]
  }

  case class WithdrawAdvicePayload(withdrawRequest: WithdrawRequest)

  object WithdrawAdvicePayload {
    implicit val format: OFormat[WithdrawAdvicePayload] = Json.format[WithdrawAdvicePayload]
  }
}
