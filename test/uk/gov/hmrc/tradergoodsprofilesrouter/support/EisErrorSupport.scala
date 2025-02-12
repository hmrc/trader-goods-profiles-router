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

package uk.gov.hmrc.tradergoodsprofilesrouter.support

import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, FORBIDDEN}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse

trait EisErrorSupport {

  private val genericErrorMessage = "There was an error"

  val badGatewayEISError: EisHttpErrorResponse =
    EisHttpErrorResponse(BAD_GATEWAY, ErrorResponse("correlationId", s"$BAD_GATEWAY", genericErrorMessage, None))
  val badRequestEISError: EisHttpErrorResponse =
    EisHttpErrorResponse(BAD_REQUEST, ErrorResponse("correlationId", s"$BAD_REQUEST", genericErrorMessage, None))
  val forbiddenEISError: EisHttpErrorResponse  = EisHttpErrorResponse(
    FORBIDDEN,
    ErrorResponse("correlationId", s"$FORBIDDEN", "Forbidden Error")
  )

}
