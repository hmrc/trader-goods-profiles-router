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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.{InvalidRequestParameters, UnexpectedErrorCode}

case class ErrorResponse(
  correlationId: String,
  code: String,
  message: String,
  errors: Option[Seq[Error]] = None
)

case class Error(code: String, message: String, errorNumber: Int)

object ErrorResponse {
  implicit val format: OFormat[ErrorResponse] = Json.format[ErrorResponse]
}

object Error {
  implicit val format: OFormat[Error] = Json.format[Error]

  def invalidRequestParameterError(message: String, errorNumber: Int) =
    Error(InvalidRequestParameters, message, errorNumber)

  def unexpectedError(message: String, errorNumber: Int) =
    Error(UnexpectedErrorCode, message, errorNumber)
}
