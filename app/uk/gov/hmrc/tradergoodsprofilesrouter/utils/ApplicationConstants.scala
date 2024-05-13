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
  val UnexpectedErrorCode          = "UNEXPECTED_ERROR"
  val UnexpectedErrorMessage       = "Unexpected Error"
  val InternalErrorResponseCode    = "INTERNAL_ERROR_RESPONSE"
  val InternalErrorResponseMessage = "Internal Error Response"
  val UnauthorizedCode             = "UNAUTHORIZED"
  val UnauthorizedMessage          = "Unauthorized"
  val BadRequestCode               = "BAD_REQUEST"
  val InvalidOrEmptyPayloadCode    = "INVALID_OR_EMPTY_PAYLOAD"
  val InvalidOrEmptyPayloadMessage = "Invalid Response Payload or Empty payload"
  val BadRequestMessage            = "Bad Request"
  val MissingHeaderClientId        = "Missing mandatory header X-Client-Id"
  val InvalidRequestParameterCode  = "INVALID_REQUEST_PARAMETER"
  val NotFoundMessage              = "Not Found"
  val NotFoundCode                 = "NOT_FOUND"
  val ForbiddenCode                = "FORBIDDEN"
  val ForbiddenMessage             = "Forbidden"
  val MethodNotAllowedCode         = "METHOD_NOT_ALLOWED"
  val MethodNotAllowedMessage      = "Method Not Allowed"
  val ServiceUnavailableCode       = "SERVICE_UNAVAILABLE"
  val ServiceUnavailableMessage    = "Service Unavailable"
  val BadGatewayCode               = "BAD_GATEWAY"
  val BadGatewayMessage            = "Bad Gateway"
  val UnknownCode                  = "UNKNOWN"
  val UnknownMessage               = "Unknown Error"
  val InvalidOrMissingEori         = "006 - Missing or invalid mandatory request parameter EORI"
  val EoriDoesNotExists            = "007 - EORI does not exist in the database"
  val InvalidRecordId              = "025 - Invalid request parameter recordId"
  val RecordIdDoesNotExists        = "026 - recordId does not exist in the database"
  val InternalServerErrorCode      = "INTERNAL_SERVER_ERROR"
  val InternalServerErrorMessage   = "Internal Server Error"
}
