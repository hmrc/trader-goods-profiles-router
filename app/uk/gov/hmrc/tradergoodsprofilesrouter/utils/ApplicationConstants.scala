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
  val UnexpectedErrorCode                                  = "UNEXPECTED_ERROR"
  val UnexpectedErrorMessage                               = "Unexpected Error"
  val InternalErrorResponseCode                            = "INTERNAL_ERROR_RESPONSE"
  val InternalErrorResponseMessage                         = "Internal Error Response"
  val UnauthorizedCode                                     = "UNAUTHORIZED"
  val UnauthorizedMessage                                  = "Unauthorized"
  val BadRequestCode                                       = "BAD_REQUEST"
  val InvalidOrEmptyPayloadCode                            = "INVALID_OR_EMPTY_PAYLOAD"
  val InvalidOrEmptyPayloadMessage                         = "Invalid Response Payload or Empty payload"
  val BadRequestMessage                                    = "Bad Request"
  val MissingHeaderClientId                                = "Missing mandatory header X-Client-Id"
  val InvalidRequestParameterCode                          = "INVALID_REQUEST_PARAMETER"
  val NotFoundMessage                                      = "Not Found"
  val NotFoundCode                                         = "NOT_FOUND"
  val ForbiddenCode                                        = "FORBIDDEN"
  val ForbiddenMessage                                     = "Forbidden"
  val MethodNotAllowedCode                                 = "METHOD_NOT_ALLOWED"
  val MethodNotAllowedMessage                              = "Method Not Allowed"
  val ServiceUnavailableCode                               = "SERVICE_UNAVAILABLE"
  val ServiceUnavailableMessage                            = "Service Unavailable"
  val BadGatewayCode                                       = "BAD_GATEWAY"
  val BadGatewayMessage                                    = "Bad Gateway"
  val UnknownCode                                          = "UNKNOWN"
  val UnknownMessage                                       = "Unknown Error"
  val InvalidOrMissingEori                                 = "006 - Missing or invalid mandatory request parameter EORI"
  val EoriDoesNotExists                                    = "007 - EORI does not exist in the database"
  val InvalidRecordId                                      = "025 - Invalid request parameter recordId"
  val RecordIdDoesNotExists                                = "026 - recordId does not exist in the database"
  val InvalidLastUpdatedDate                               = "028 - Invalid optional request parameter lastUpdatedDate"
  val InvalidPage                                          = "029 - Invalid optional request parameter page"
  val InvalidSize                                          = "030 - Invalid optional request parameter size"
  val InvalidOrMissingActorId                              = "008 - Missing or invalid mandatory request parameter ActorId"
  val InvalidOrMissingTraderRef                            = "009 - Missing or invalid mandatory request parameter traderRef"
  val TraderRefIsNotUnique                                 = "010 - traderRef is not unique for all the provided traderEori goods records"
  val InvalidOrMissingComcode                              = "011 - Missing or invalid mandatory request parameter comcode"
  val InvalidOrMissingGoodsDescription                     = "012 - Missing or invalid mandatory request parameter goodsDescription"
  val InvalidOrMissingCountryOfOrigin                      = "013 - Missing or invalid mandatory request parameter countryOfOrigin"
  val InvalidOrMissingCategory                             = "014 - Missing or invalid mandatory request parameter category"
  val InvalidOrMissingAssessmentId                         = "015 - Invalid optional request parameter assessments[].assessmentId"
  val InvalidAssessmentPrimaryCategory                     = "016 - Invalid optional request parameter assessments[]. primaryCategory"
  val InvalidAssessmentPrimaryCategoryConditionType        =
    "017 - Invalid optional request parameter assessments[]. primaryCategory.condition.type"
  val InvalidAssessmentPrimaryCategoryConditionId          =
    "018 - Invalid optional request parameter assessments[]. primaryCategory.condition. conditionId"
  val InvalidAssessmentPrimaryCategoryConditionDescription =
    "019 - Invalid optional request parameter assessments[]. primaryCategory.condition. conditionDescription"
  val InvalidAssessmentPrimaryCategoryConditionTraderText  =
    "020 - Invalid optional request parameter assessments[]. primaryCategory.condition. conditionTraderText"
  val InvalidOrMissingSupplementaryUnit                    = "021 - Invalid optional request parameter supplementaryUnit"
  val InvalidOrMissingMeasurementUnit                      = "022 - Invalid optional request parameter measurementUnit"
  val InvalidOrMissingComcodeEffectiveFromDate             =
    "023 - Missing or invalid mandatory request parameter comcodeEffectiveFromDate"
  val InvalidOrMissingComcodeEffectiveToDate               = "024 - Invalid optional request parameter comcodeEffectiveToDate"
  val InternalServerErrorCode                              = "INTERNAL_SERVER_ERROR"
  val InternalServerErrorMessage                           = "Internal Server Error"
  val InvalidRequestObjectCode                             = "INVALID_REQUEST_OBJECT"
}
