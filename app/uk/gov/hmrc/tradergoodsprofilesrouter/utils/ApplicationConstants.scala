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

package uk.gov.hmrc.tradergoodsprofilesrouter.utils

object ApplicationConstants {
  val UNEXPECTED_ERROR_CODE            = "UNEXPECTED_ERROR"
  val UNEXPECTED_ERROR_MESSAGE         = "Unexpected Error"
  val INTERNAL_ERROR_RESPONSE_CODE     = "INTERNAL_ERROR_RESPONSE"
  val INTERNAL_ERROR_RESPONSE_MESSAGE  = "Internal Error Response"
  val UNAUTHORIZED_CODE                = "UNAUTHORIZED"
  val UNAUTHORIZED_MESSAGE             = "Unauthorized"
  val BAD_REQUEST_CODE                 = "BAD_REQUEST"
  val INVALID_OR_EMPTY_PAYLOAD_CODE    = "INVALID_OR_EMPTY_PAYLOAD"
  val INVALID_OR_EMPTY_PAYLOAD_MESSAGE = "Invalid Response Payload or Empty payload"
  val BAD_REQUEST_MESSAGE              = "Bad Request"
  val INVALID_REQUEST_PARAMETER_CODE   = "INVALID_REQUEST_PARAMETER"
  val NOT_FOUND_MESSAGE                = "Not Found"
  val NOT_FOUND_CODE                   = "NOT_FOUND"
  val FORBIDDEN_CODE                   = "FORBIDDEN"
  val FORBIDDEN_MESSAGE                = "Forbidden"
  val METHOD_NOT_ALLOWED_CODE          = "METHOD_NOT_ALLOWED"
  val METHOD_NOT_ALLOWED_MESSAGE       = "Method Not Allowed"
  val SERVICE_UNAVAILABLE_CODE         = "SERVICE_UNAVAILABLE"
  val SERVICE_UNAVAILABLE_MESSAGE      = "Service Unavailable"
  val BAD_GATEWAY_CODE                 = "BAD_GATEWAY"
  val BAD_GATEWAY_MESSAGE              = "Bad Gateway"
  val UNKNOWN_CODE                     = "UNKNOWN"
  val UNKNOWN_MESSAGE                  = "Unknown Error"
  val INVALID_OR_MISSING_EORI          = "006 - Missing or invalid mandatory request parameter EORI"
  val EORI_DOES_NOT_EXISTS             = "007 - eori doesn’t exist in the database"
  val INVALID_RECORD_ID                = "025 - Invalid request parameter recordId"
  val RECORD_ID_DOES_NOT_EXISTS        = "026 - recordId doesn’t exist in the database"
  val INTERNAL_SERVER_ERROR_CODE       = "INTERNAL_SERVER_ERROR"
  val INTERNAL_SERVER_ERROR_MESSAGE    = "Internal Server Error"
}
