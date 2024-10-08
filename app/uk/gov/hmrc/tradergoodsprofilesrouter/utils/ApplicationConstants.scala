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
  val InvalidHeader                                        = "INVALID_HEADER"
  val InvalidActorIdQueryParameter                         = "Query parameter actorId is in the wrong format"
  val InvalidRequestParameters                             = "INVALID_REQUEST_PARAMETER"
  val InvalidQueryParameter                                = "INVALID_QUERY_PARAMETER"
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
  val MissingHeaderClientId                                = "Missing mandatory header X-Client-ID"
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
  val InvalidOrMissingEori                                 =
    "Mandatory field eori was missing from body or is in the wrong format"
  val EoriDoesNotExists                                    = "EORI number does not have a TGP"
  val InvalidOrMissingActorId                              = "Mandatory field actorId was missing from body or is in the wrong format"
  val InvalidOrMissingTraderRef                            = "Mandatory field traderRef was missing from body or is in the wrong format"
  val InvalidOrMissingOptionalTraderRef                    = "Optional field traderRef was missing from body or is in the wrong format"
  val TraderRefIsNotUnique                                 = "Trying to create or update a record with a duplicate traderRef"
  val InvalidOrMissingComcode                              = "Mandatory field comcode was missing from body or is in the wrong format"
  val InvalidOrMissingOptionalComcode                      = "Optional field comcode was missing from body or is in the wrong format"
  val InvalidOrMissingGoodsDescription                     =
    "Mandatory field goodsDescription was missing from body or is in the wrong format"
  val InvalidOrMissingOptionalGoodsDescription             =
    "Optional field goodsDescription was missing from body or is in the wrong format"
  val InvalidOrMissingCountryOfOrigin                      =
    "Mandatory field countryOfOrigin was missing from body or is in the wrong format"
  val InvalidOrMissingOptionalCountryOfOrigin              =
    "Optional field countryOfOrigin was missing from body or is in the wrong format"
  val InvalidCategory                                      = "Optional field category is in the wrong format"
  val InvalidOrMissingAssessmentId                         = "Optional field assessmentId is in the wrong format"
  val InvalidAssessmentPrimaryCategory                     = "Optional field primaryCategory is in the wrong format"
  val InvalidAssessmentPrimaryCategoryConditionType        =
    "Optional field type is in the wrong format"
  val InvalidAssessmentPrimaryCategoryConditionId          =
    "Optional field conditionId is in the wrong format"
  val InvalidAssessmentPrimaryCategoryConditionDescription =
    "Optional field conditionDescription is in the wrong format"
  val InvalidAssessmentPrimaryCategoryConditionTraderText  =
    "Optional field conditionTraderText is in the wrong format"
  val InvalidOrMissingSupplementaryUnit                    = "Optional field supplementaryUnit is in the wrong format"
  val InvalidOrMissingMeasurementUnit                      = "Optional field measurementUnit is in the wrong format"
  val InvalidOrMissingComcodeEffectiveFromDate             =
    "Mandatory field comcodeEffectiveFromDate was missing from body or is in the wrong format"
  val InvalidOrMissingOptionalComcodeEffectiveFromDate     =
    "Optional field comcodeEffectiveFromDate was missing from body or is in the wrong format"
  val InvalidOrMissingComcodeEffectiveToDate               = "Optional field comcodeEffectiveToDate is in the wrong format"
  val InvalidRecordId                                      = "The recordId has been provided in the wrong format"
  val RecordIdDoesNotExists                                = "The requested recordId doesn’t exist"
  val InvalidLastUpdatedDate                               = "The URL parameter lastUpdatedDate is in the wrong format"
  val InvalidPage                                          = "The URL parameter page is in the wrong format"
  val InvalidSize                                          = "The URL parameter size is in the wrong format"
  val RecordRemovedAndCanNotBeUpdatedMessage               = "This record has been removed and cannot be updated"
  val AdviceRequestInProgressMessage                       =
    "There is an ongoing accreditation request and the record can not be updated"
  val AdviceRequestRejectionMessage                        = "There is an ongoing advice request and a new request cannot be requested."

  val InvalidOrMissingCorrelationID      =
    "X-Correlation-ID was missing from Header or is in the wrong format"
  val InvalidOrMissingRequestDate        =
    "Request Date was missing from Header or is in the wrong format"
  val InvalidOrMissingForwardedHost      =
    "X-Forwarded-Host was missing from Header or is in the wrong format"
  val InvalidOrMissingContentType        =
    "Content-Type was missing from Header or is in the wrong format"
  val InvalidOrMissingAccept             =
    "Accept was missing from Header or is in the wrong format"
  val InvalidOrMissingReceiptDate        =
    "Mandatory field receiptDate was missing from body"
  val InvalidOrMissingTraderEORI         =
    "The eori has been provided in the wrong format"
  val InvalidOrMissingRequestorName      =
    "Mandatory field RequestorName was missing from body or is in the wrong format"
  val InvalidOrMissingRequestorEmail     =
    "Mandatory field RequestorEmail was missing from body or is in the wrong format"
  val InvalidOrMissingUkimsAuthorisation =
    "Mandatory field ukimsNumber was missing from body or in the wrong format"
  val InvalidOrMissingGoodsItems         =
    "Mandatory field goodsItems was missing from body"
  val InvalidOrMissingPublicRecordID     =
    "The recordId has been provided in the wrong format"
  val InvalidOrMissingTraderReference    =
    "Mandatory field traderReference was missing from body"
  val InvalidOrMissingCommodityCode      =
    "Mandatory field commodityCode was missing from body"

  val InvalidOrMissingUkimsNumberMessage = "Mandatory field ukimsNumber was missing from body or is in the wrong format"
  val InvalidOrMissingNirmsNumberMessage = "Optional field nirmsNumber is in the wrong format"
  val InvalidOrMissingNiphlNumberMessage = "Optional field niphlNumber is in the wrong format"

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
  val InvalidCategoryCode                                      = "014"
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
  val AdviceRequestInProgressCode                              = "027"
  val InvalidLastUpdatedDateCode                               = "028"
  val InvalidPageCode                                          = "029"
  val InvalidSizeCode                                          = "030"
  val RecordRemovedAndCanNotBeUpdatedCode                      = "031"
  val InvalidMissingRequestorNameCode                          = "037"
  val InvalidMissingRequestorEmailCode                         = "038"
  val InvalidOrMissingUkimsNumberCode                          = "033"
  val InvalidOrMissingNirmsNumberCode                          = "034"
  val InvalidOrMissingNiphlNumberCode                          = "035"
  val InvalidRequestAdviceNumberCode                           = "1015"

  val InvalidOrMissingCorrelationIdCode      = "E001"
  val InvalidOrMissingRequestDateCode        = "E002"
  val InvalidOrMissingForwardedHostCode      = "E003"
  val InvalidOrMissingContentTypeCode        = "E004"
  val InvalidOrMissingAcceptCode             = "E005"
  val InvalidOrMissingReceiptDateCode        = "E006"
  val InvalidOrMissingTraderEORICode         = "E007"
  val InvalidOrMissingRequestorNameCode      = "E008"
  val InvalidOrMissingRequestorEmailCode     = "E009"
  val InvalidOrMissingUkimsAuthorisationCode = "E010"
  val InvalidOrMissingGoodsItemsCode         = "E011"
  val InvalidOrMissingPublicRecordIDCode     = "E012"
  val InvalidOrMissingTraderReferenceCode    = "E013"
  val InvalidOrMissinggoodsDescriptionCode   = "E014"
  val InvalidOrMissingCommodityCodeCode      = "E015"
  val invalidWithdrawReasonCode              = 1018
}

object DownloadTraderDataConstants {
  val InvalidCorrelationHeaderErrorCode = "001"
  val InvalidCorrelationHeaderErrorMsg  =
    "X-Correlation-ID was missing from Header or is in the wrong format"

  val InvalidDateHeaderErrorCode = "002"
  val InvalidDateHeaderErrorMsg  =
    "Request Date was missing from Header or is in the wrong format"

  val InvalidForwardedHostCode = "005"
  val InvalidForwardedHostMsg  =
    "X-Forwarded-Host was missing from Header or is in the wrong format"

  val InvalidOrMissingEoriCode = "006"
  val InvalidOrMissingEoriMsg  =
    "The EORI number has been provided in the wrong format"

  val EoriDoesNotExistsCode = "007"
  val EoriDoesNotExistsMsg  = "EORI number does not have a TGP"

  val EoriIsNotLinkedToAnyRecord = "037"
  val EoriIsNotLinkedToAnyMsg    = "EORI number is not linked to any records in the database"
}

object CreateTraderProfile {
  val EoriAlreadyExistsCode    = "038"
  val EoriAlreadyExistsMessage = "Eori already associated with another profile"
}

object WithdrawAdviceConstant {
  val InvalidOrMissingCorrelationIdCode: String      = "E001"
  val InvalidOrMissingCorrelationIdMsg: String       =
    "X-Correlation-ID was missing from Header or is in the wrong format"
  val InvalidOrMissingCorrelationIdResponseCode: Int = 1

  val InvalidOrMissingForwardedHostCode: String      = "E002"
  val InvalidOrMissingForwardedHostMsg: String       =
    "X-Forwarded-Host was missing from Header or is in the wrong format"
  val InvalidOrMissingForwardedHostResponseCode: Int = 5

  val InvalidOrMissingContentTypeCode: String      = "E003"
  val InvalidOrMissingContentTypeMsg: String       =
    "Content-Type was missing from Header or is in the wrong format"
  val InvalidOrMissingContentTypeResponseCode: Int = 3

  val InvalidOrMissingAcceptCode: String      = "E004"
  val InvalidOrMissingAcceptMsg: String       =
    "Accept was missing from Header or is in the wrong format"
  val InvalidOrMissingAcceptResponseCode: Int = 4

  val MissingWithdrawDateCode: String      = "E005"
  val MissingWithdrawDateMsg: String       =
    "Mandatory withdrawDate was missing from body"
  val MissingWithdrawDateResponseCode: Int = 1013

  val MissingGoodsItemsCode: String      = "E006"
  val MissingGoodsItemsMsg: String       =
    "Mandatory goodsItems was missing from body"
  val MissingGoodsItemsResponseCode: Int = 1014

  val InvalidRecordIdCode: String      = "E007"
  val InvalidRecordIdMsg: String       =
    "The recordId has been provided in the wrong format"
  val InvalidRecordIdResponseCode: Int = 25

  val NoCaseFoundCode: String      = "E008"
  val NoCaseFoundMsg: String       =
    "There isn't an outstanding request for this record"
  val NoCaseFoundResponseCode: Int = 1019

  val DecisionAlreadyMadeCode: String      = "E009"
  val DecisionAlreadyMadeMsg: String       =
    "The request has already been completed and a new request cannot be requested"
  val DecisionAlreadyMadeResponseCode: Int = 1017

  val InvalidWithdrawReasonMessage: String     = "Digital checked that withdraw reason is > 512"
  val InvalidWithdrawReasonNullMessage: String = "Digital checked that withdraw reason is < 1"
}

object GetProfileSpecificError {
  val GetProfileInvalidRequestCode               = "000"
  val GetProfileInvalidRequestMessage            = "Invalid Request"
  val GetProfileInvalidCorrelationIdCode: String = "001"
  val GetProfileInvalidDateCode                  = "002"
  val GetProfileInvalidContentTypeCode: String   = "003"
  val GetProfileInvalidAcceptCode: String        = "004"
  val GetProfileInvalidForwardedHostCode: String = "005"
}
