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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis

import play.api.libs.json.{JsError, JsSuccess, JsValue, Reads}

import java.time.Instant

case class ErrorDetail(
  timestamp: Instant,
  correlationId: String,
  errorCode: String,
  errorMessage: String,
  source: Option[String],
  sourceFaultDetail: Option[SourceFaultDetail] = None
)

object ErrorDetail {
  implicit val errorDetail: Reads[ErrorDetail] = (json: JsValue) =>
    try JsSuccess(
      ErrorDetail(
        (json \ "errorDetail" \ "timestamp").as[Instant],
        (json \ "errorDetail" \ "correlationId").as[String],
        (json \ "errorDetail" \ "errorCode").as[String],
        (json \ "errorDetail" \ "errorMessage").as[String],
        (json \ "errorDetail" \ "source").asOpt[String],
        (json \ "errorDetail" \ "sourceFaultDetail").asOpt[SourceFaultDetail]
      )
    )
    catch {
      case e: Exception => JsError("Invalid json")
    }
}
