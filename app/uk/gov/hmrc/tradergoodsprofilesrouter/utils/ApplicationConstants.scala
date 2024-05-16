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
  val UnexpectedErrorCode                                      = "UNEXPECTED_ERROR"
  val UnexpectedErrorMessage                                   = "Unexpected Error"
  val InternalErrorResponseCode                                = "INTERNAL_ERROR_RESPONSE"
  val InternalErrorResponseMessage                             = "Internal Error Response"
  val UnauthorizedCode                                         = "UNAUTHORIZED"
  val UnauthorizedMessage                                      = "Unauthorized"
  val BadRequestCode                                           = "BAD_REQUEST"
  val InvalidOrEmptyPayloadCode                                = "INVALID_OR_EMPTY_PAYLOAD"
  val InvalidOrEmptyPayloadMessage                             = "Invalid Response Payload or Empty payload"
  val BadRequestMessage                                        = "Bad Request"
  val MissingHeaderClientId                                    = "Missing mandatory header X-Client-Id"
  val NotFoundMessage                                          = "Not Found"
  val NotFoundCode                                             = "NOT_FOUND"
  val ForbiddenCode                                            = "FORBIDDEN"
  val ForbiddenMessage                                         = "Forbidden"
  val MethodNotAllowedCode                                     = "METHOD_NOT_ALLOWED"
  val MethodNotAllowedMessage                                  = "Method Not Allowed"
  val ServiceUnavailableCode                                   = "SERVICE_UNAVAILABLE"
  val ServiceUnavailableMessage                                = "Service Unavailable"
  val BadGatewayCode                                           = "BAD_GATEWAY"
  val BadGatewayMessage                                        = "Bad Gateway"
  val UnknownCode                                              = "UNKNOWN"
  val UnknownMessage                                           = "Unknown Error"
  val InvalidOrMissingEori                                     = "Mandatory field eori was missing from body"
  val EoriDoesNotExists                                        = "EORI number does not have a TGP"
  val InvalidOrMissingActorId                                  = "Mandatory field actorId was missing from body"
  val InvalidOrMissingTraderRef                                = "Mandatory field traderRef was missing from body"
  val TraderRefIsNotUnique                                     = "Trying to create or update a record with a duplicate traderRef"
  val InvalidOrMissingComcode                                  = "Mandatory field comcode was missing from body"
  val InvalidOrMissingGoodsDescription                         = "Mandatory field goodsDescription was missing from body"
  val InvalidOrMissingCountryOfOrigin                          = "Mandatory field countryOfOrigin was missing from body"
  val InvalidOrMissingCategory                                 = "Mandatory field category was missing from body"
  val InvalidOrMissingAssessmentId                             = "Optional field assessmentId is in the wrong format"
  val InvalidAssessmentPrimaryCategory                         = "Optional field primaryCategory is in the wrong format"
  val InvalidAssessmentPrimaryCategoryConditionType            =
    "Optional field type is in the wrong format"
  val InvalidAssessmentPrimaryCategoryConditionId              =
    "Optional field conditionId is in the wrong format"
  val InvalidAssessmentPrimaryCategoryConditionDescription     =
    "Optional field conditionDescription is in the wrong format"
  val InvalidAssessmentPrimaryCategoryConditionTraderText      =
    "Optional field conditionTraderText is in the wrong format"
  val InvalidOrMissingSupplementaryUnit                        = "Optional field supplementaryUnit is in the wrong format"
  val InvalidOrMissingMeasurementUnit                          = "Optional field measurementUnit is in the wrong format"
  val InvalidOrMissingComcodeEffectiveFromDate                 =
    "Mandatory field comcodeEffectiveFromDate was missing from body"
  val InvalidOrMissingComcodeEffectiveToDate                   = "Optional field comcodeEffectiveToDate is in the wrong format"
  val InvalidRecordId                                          = "The recordId has been provided in the wrong format"
  val RecordIdDoesNotExists                                    = "The requested recordId to update doesn’t exist"
  val InvalidLastUpdatedDate                                   = "The URL parameter lastUpdatedDate is in the wrong format"
  val InvalidPage                                              = "The URL parameter page is in the wrong format"
  val InvalidSize                                              = "The URL parameter size is in the wrong format"
  val InternalServerErrorCode                                  = "INTERNAL_SERVER_ERROR"
  val InternalServerErrorMessage                               = "Internal Server Error"
  val InvalidOrMissingEoriCode                                 = "006"
  val EoriDoesNotExistsCode                                    = "007"
  val InvalidOrMissingActorIdCode                              = "008"
  val InvalidOrMissingTraderRefCode                            = "009"
  val TraderRefIsNotUniqueCode                                 = "010"
  val InvalidOrMissingComcodeCode                              = "011"
  val InvalidOrMissingGoodsDescriptionCode                     = "012"
  val InvalidOrMissingCountryOfOriginCode                      = "013"
  val InvalidOrMissingCategoryCode                             = "014"
  val InvalidOrMissingAssessmentIdCode                         = "015"
  val InvalidAssessmentPrimaryCategoryCode                     = "016"
  val InvalidAssessmentPrimaryCategoryConditionTypeCode        = "017"
  val InvalidAssessmentPrimaryCategoryConditionIdCode          = "018"
  val InvalidAssessmentPrimaryCategoryConditionDescriptionCode = "019"
  val InvalidAssessmentPrimaryCategoryConditionTraderTextCode  = "020"
  val InvalidOrMissingSupplementaryUnitCode                    = "021"
  val InvalidOrMissingMeasurementUnitCode                      = "022"
  val InvalidOrMissingComcodeEffectiveFromDateCode             = "023"
  val InvalidOrMissingComcodeEffectiveToDateCode               = "024"
  val InvalidRecordIdCode                                      = "025"
  val RecordIdDoesNotExistsCode                                = "026"
  val InvalidLastUpdatedDateCode                               = "028"
  val InvalidPageCode                                          = "029"
  val InvalidSizeCode                                          = "030"
}
