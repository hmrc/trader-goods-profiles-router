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

package uk.gov.hmrc.tradergoodsprofilesrouter.service

import cats.data.EitherT
import com.google.inject.{ImplementedBy, Inject}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{ErrorDetail, GetEisRecordsResponse, GoodsItemRecords}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[RouterServiceImpl])
trait RouterService {
  def fetchRecord(
    eori: String,
    recordId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): EitherT[Future, Result, GoodsItemRecords]

  def fetchRecords(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): EitherT[Future, Result, GetEisRecordsResponse]

  def createRecord(
    request: CreateRecordRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): EitherT[Future, Result, CreateRecordResponse]
}

class RouterServiceImpl @Inject() (eisConnector: EISConnector, uuidService: UuidService) extends RouterService {

  override def fetchRecord(eori: String, recordId: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, Result, GoodsItemRecords] = {
    implicit val correlationId: String = uuidService.uuid
    EitherT(
      eisConnector
        .fetchRecord(eori, recordId)
        .map {
          case Right(response) => Right(response.goodsItemRecords.head)
          case Left(error)     =>
            Left(error)
        }
        .recover { case ex: Throwable =>
          Left(InternalServerError(ex.getMessage))
        }
    )
  }

  override def fetchRecords(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, Result, GetEisRecordsResponse] = {
    implicit val correlationId: String = uuidService.uuid
    EitherT(
      eisConnector
        .fetchRecords(eori, lastUpdatedDate, page, size)
        .map {
          case Right(response) => Right(response)
          case Left(error)     => Left(error)
        }
    )
  }
  private def determine400Error(correlationId: String, message: String): ErrorResponse =
    Json.parse(message).validate[ErrorDetail] match {
      case JsSuccess(detail, _) =>
        setBadRequestResponse(correlationId, detail)
      case JsError(_)           =>
        ErrorResponse(
          correlationId,
          UnexpectedErrorCode,
          UnexpectedErrorMessage
        )
    }

  private def determine500Error(correlationId: String, message: String): ErrorResponse =
    Json.parse(message).validate[ErrorDetail] match {
      case JsSuccess(detail, _) =>
        detail.errorCode match {
          case "200" | "201" =>
            ErrorResponse(
              correlationId,
              InvalidOrEmptyPayloadCode,
              InvalidOrEmptyPayloadMessage
            )
          case "400"         =>
            ErrorResponse(
              correlationId,
              InternalErrorResponseCode,
              InternalErrorResponseMessage
            )
          case "401"         =>
            ErrorResponse(
              correlationId,
              UnauthorizedCode,
              UnauthorizedMessage
            )
          case "404"         =>
            ErrorResponse(correlationId, NotFoundCode, NotFoundMessage)
          case "405"         =>
            ErrorResponse(
              correlationId,
              MethodNotAllowedCode,
              MethodNotAllowedMessage
            )
          case "500"         =>
            ErrorResponse(
              correlationId,
              InternalServerErrorCode,
              InternalServerErrorMessage
            )
          case "502"         =>
            ErrorResponse(
              correlationId,
              BadGatewayCode,
              BadGatewayMessage
            )
          case "503"         =>
            ErrorResponse(
              correlationId,
              ServiceUnavailableCode,
              ServiceUnavailableMessage
            )
          case _             =>
            ErrorResponse(correlationId, UnknownCode, UnknownMessage)
        }
      case JsError(_)           =>
        ErrorResponse(
          correlationId,
          UnexpectedErrorCode,
          UnexpectedErrorMessage
        )
    }

  private def setBadRequestResponse(correlationId: String, detail: ErrorDetail): ErrorResponse =
    ErrorResponse(
      correlationId,
      BadRequestCode,
      BadRequestMessage,
      detail.sourceFaultDetail.map { sfd =>
        sfd.detail.get.map(detail => parseFaultDetail(detail, correlationId))
      }
    )

  private def parseFaultDetail(rawDetail: String, correlationId: String): Error = {
    val regex = """error:\s*(\d+),\s*message:\s*(.*)""".r

    rawDetail match {
      case regex(code, _) =>
        code match {
          case InvalidOrMissingEoriCode                                 =>
            Error(
              InvalidOrMissingEoriCode,
              InvalidOrMissingEori
            )
          case EoriDoesNotExistsCode                                    =>
            Error(EoriDoesNotExistsCode, EoriDoesNotExists)
          case InvalidOrMissingActorIdCode                              =>
            Error(InvalidOrMissingActorIdCode, InvalidOrMissingActorId)
          case InvalidOrMissingTraderRefCode                            =>
            Error(InvalidOrMissingTraderRefCode, InvalidOrMissingTraderRef)
          case TraderRefIsNotUniqueCode                                 =>
            Error(TraderRefIsNotUniqueCode, TraderRefIsNotUnique)
          case InvalidOrMissingComcodeCode                              =>
            Error(InvalidOrMissingComcodeCode, InvalidOrMissingComcode)
          case InvalidOrMissingGoodsDescriptionCode                     =>
            Error(
              InvalidOrMissingGoodsDescriptionCode,
              InvalidOrMissingGoodsDescription
            )
          case InvalidOrMissingCountryOfOriginCode                      =>
            Error(
              InvalidOrMissingCountryOfOriginCode,
              InvalidOrMissingCountryOfOrigin
            )
          case InvalidOrMissingCategoryCode                             =>
            Error(InvalidOrMissingCategoryCode, InvalidOrMissingCategory)
          case InvalidOrMissingAssessmentIdCode                         =>
            Error(
              InvalidOrMissingAssessmentIdCode,
              InvalidOrMissingAssessmentId
            )
          case InvalidAssessmentPrimaryCategoryCode                     =>
            Error(
              InvalidAssessmentPrimaryCategoryCode,
              InvalidAssessmentPrimaryCategory
            )
          case InvalidAssessmentPrimaryCategoryConditionTypeCode        =>
            Error(
              InvalidAssessmentPrimaryCategoryConditionTypeCode,
              InvalidAssessmentPrimaryCategoryConditionType
            )
          case InvalidAssessmentPrimaryCategoryConditionIdCode          =>
            Error(
              InvalidAssessmentPrimaryCategoryConditionIdCode,
              InvalidAssessmentPrimaryCategoryConditionId
            )
          case InvalidAssessmentPrimaryCategoryConditionDescriptionCode =>
            Error(
              InvalidAssessmentPrimaryCategoryConditionDescriptionCode,
              InvalidAssessmentPrimaryCategoryConditionDescription
            )
          case InvalidAssessmentPrimaryCategoryConditionTraderTextCode  =>
            Error(
              InvalidAssessmentPrimaryCategoryConditionTraderTextCode,
              InvalidAssessmentPrimaryCategoryConditionTraderText
            )
          case InvalidOrMissingSupplementaryUnitCode                    =>
            Error(
              InvalidOrMissingSupplementaryUnitCode,
              InvalidOrMissingSupplementaryUnit
            )
          case InvalidOrMissingMeasurementUnitCode                      =>
            Error(
              InvalidOrMissingMeasurementUnitCode,
              InvalidOrMissingMeasurementUnit
            )
          case InvalidOrMissingComcodeEffectiveFromDateCode             =>
            Error(
              InvalidOrMissingComcodeEffectiveFromDateCode,
              InvalidOrMissingComcodeEffectiveFromDate
            )
          case InvalidOrMissingComcodeEffectiveToDateCode               =>
            Error(
              InvalidOrMissingComcodeEffectiveToDateCode,
              InvalidOrMissingComcodeEffectiveToDate
            )
          case InvalidRecordIdCode                                      =>
            Error(InvalidRecordIdCode, InvalidRecordId)
          case RecordIdDoesNotExistsCode                                =>
            Error(RecordIdDoesNotExistsCode, RecordIdDoesNotExists)
          case InvalidLastUpdatedDateCode                               =>
            Error(InvalidLastUpdatedDateCode, InvalidLastUpdatedDate)
          case InvalidPageCode                                          =>
            Error(InvalidPageCode, InvalidPage)
          case InvalidSizeCode                                          =>
            Error(InvalidSizeCode, InvalidSize)
          case _                                                        => Error(UnexpectedErrorCode, UnexpectedErrorMessage)
        }
      case _              =>
        throw new IllegalArgumentException(s"Unable to parse fault detail for correlation Id: $correlationId")
    }
  }
}
