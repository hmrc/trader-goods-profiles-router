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

package uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action

import cats.implicits.catsSyntaxTuple2Parallel
import cats.syntax.all._
import org.apache.commons.validator.routines.EmailValidator
import play.api.libs.functional.syntax.toApplicativeOps
import play.api.libs.json.Json.toJson
import play.api.libs.json.Reads.{maxLength, minLength, verifying}
import play.api.libs.json._
import play.api.mvc.Results.BadRequest
import play.api.mvc.{BaseController, Request, Result}
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.{BadRequestErrorResponse, ValidatedQueryParameters, ValidatedWithdrawAdviceQueryParameters, WithdrawReasonCharLimit, extractSimplePaths, isValidActorId}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.WithdrawAdviceConstant.invalidWithdrawReasonMessage

import java.util.{Locale, UUID}
import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.util.matching.Regex

trait ValidationRules {
  this: BaseController =>

  def uuidService: UuidService

  implicit def ec: ExecutionContext

  protected def validateClientId(implicit request: Request[_]): Either[Result, String] =
    request.headers
      .get(HeaderNames.ClientId)
      .toRight(
        BadRequestErrorResponse(uuidService.uuid, Seq(Error(InvalidHeader, MissingHeaderClientId, 6000))).asPresentation
      )

  protected def validateRecordId(recordId: String): Either[Error, String] =
    //todo: should this be a path parameter error instead?
    Try(UUID.fromString(recordId).toString).toOption.toRight(
      Error(
        InvalidQueryParameter,
        InvalidRecordId,
        InvalidRecordIdCode.toInt
      )
    )

  protected def validateRequestBody[A: Reads](
    fieldToErrorCodeTable: Map[String, (String, String)]
  )(implicit request: Request[JsValue]): Either[Result, A] =
    request.body
      .validate[A]
      .asEither
      .left
      .map(x =>
        BadRequestErrorResponse(
          uuidService.uuid,
          convertError[A](x, fieldToErrorCodeTable)
        ).asPresentation
      )

  protected def validateQueryParameters(
    actorId: String,
    recordId: String
  ): Either[List[Error], ValidatedQueryParameters] =
    (
      validateActorId(actorId).toEitherNec,
      validateRecordId(recordId).toEitherNec
    ).parMapN { (validatedActorId, validatedRecordId) =>
      ValidatedQueryParameters(validatedActorId, validatedRecordId)
    } leftMap { errors =>
      errors.toList
    }

  protected def validateWithdrawAdviceQueryParam(
    recordId: String
  )(implicit request: Request[_]): Either[List[Error], ValidatedWithdrawAdviceQueryParameters] =
    (
      validateWithdrawReasonQueryParam.toEitherNec,
      validateRecordId(recordId).toEitherNec
    ).parMapN { (validatedWithdrawReason, validatedRecordId) =>
      ValidatedWithdrawAdviceQueryParameters(validatedWithdrawReason, validatedRecordId)
    } leftMap { errors =>
      errors.toList
    }

  private def validateActorId(actorId: String): Either[Error, String] =
    if (isValidActorId(actorId)) Right(actorId)
    else
      Left(
        Error(
          InvalidQueryParameter,
          InvalidActorIdQueryParameter,
          InvalidOrMissingActorIdCode.toInt
        )
      )

  private def validateWithdrawReasonQueryParam(implicit request: Request[_]): Either[Error, String] =
    request
      .getQueryString("withdrawReason")
      .orElse(Some(""))
      .filter(_.length < WithdrawReasonCharLimit + 1)
      .toRight(
        Error(InvalidQueryParameter, invalidWithdrawReasonMessage, invalidWithdrawReasonCode)
      )

  private def convertError[T](
    errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])],
    fieldToErrorCodeTable: Map[String, (String, String)]
  ): Seq[Error] =
    extractSimplePaths(errors)
      .map(key => fieldToErrorCodeTable.get(key).map(res => Error.invalidRequestParameterError(res._2, res._1.toInt)))
      .toSeq
      .flatten
}

object ValidationRules {

  final case class ValidatedQueryParameters(actorId: String, recordId: String)
  final case class ValidatedWithdrawAdviceQueryParameters(withdrawReason: String, recordId: String)

  private val WithdrawReasonCharLimit = 512
  private val actorIdPattern: Regex   = raw"[A-Z]{2}\d{12,15}".r
  private val comcodePattern: Regex   = raw".{6}(.{2}(.{2})?)?".r
  private val niphlPattern: Regex     = raw"([0-9]{4,6}|[a-zA-Z]{1,2}[0-9]{5})".r

  def isValidCountryCode(rawCountryCode: String): Boolean =
    Locale.getISOCountries.toSeq.contains(rawCountryCode.toUpperCase)

  private val emailValidator: EmailValidator = EmailValidator.getInstance(true)

  object Reads {
    def lengthBetween(min: Int, max: Int): Reads[String] =
      minLength[String](min).keepAnd(maxLength[String](max))

    val validActorId: Reads[String] = verifying(isValidActorId)

    val validComcode: Reads[String] = verifying(isValidComcode)

    val validEmailAddress: Reads[String] = verifying(isValidEmailAddress)

    val validNiphl: Reads[String] = verifying(isValidNiphl)
  }

  def isValidEmailAddress(emailAddress: String): Boolean = emailValidator.isValid(emailAddress)

  def isValidActorId(actorId: String): Boolean = actorIdPattern.matches(actorId)

  def isValidComcode(comcode: String): Boolean = comcodePattern.matches(comcode)

  def isValidNiphl(niphl: String): Boolean = niphlPattern.matches(niphl)

  private def extractSimplePaths(
    errors: scala.collection.Seq[(JsPath, collection.Seq[JsonValidationError])]
  ): collection.Seq[String] =
    errors
      .map(_._1)
      .map(_.path.filter(_.isInstanceOf[KeyPathNode]))
      .map(_.mkString)

  val fieldsToErrorCode: Map[String, (String, String)] = Map(
    "/eori"                                       -> (InvalidOrMissingEoriCode, InvalidOrMissingEori),
    "/recordId"                                   -> (RecordIdDoesNotExistsCode, InvalidRecordId),
    "/actorId"                                    -> (InvalidOrMissingActorIdCode, InvalidOrMissingActorId),
    "/traderRef"                                  -> (InvalidOrMissingTraderRefCode, InvalidOrMissingTraderRef),
    "/comcode"                                    -> (InvalidOrMissingComcodeCode, InvalidOrMissingComcode),
    "/goodsDescription"                           -> (InvalidOrMissingGoodsDescriptionCode, InvalidOrMissingGoodsDescription),
    "/countryOfOrigin"                            -> (InvalidOrMissingCountryOfOriginCode, InvalidOrMissingCountryOfOrigin),
    "/category"                                   -> (InvalidOrMissingCategoryCode, InvalidOrMissingCategory),
    "/assessments/assessmentId"                   -> (InvalidOrMissingAssessmentIdCode, InvalidOrMissingAssessmentId),
    "/assessments/primaryCategory"                -> (InvalidAssessmentPrimaryCategoryCode, InvalidAssessmentPrimaryCategory),
    "/assessments/condition/type"                 -> (InvalidAssessmentPrimaryCategoryConditionTypeCode, InvalidAssessmentPrimaryCategoryConditionType),
    "/assessments/condition/conditionId"          -> (InvalidAssessmentPrimaryCategoryConditionIdCode, InvalidAssessmentPrimaryCategoryConditionId),
    "/assessments/condition/conditionDescription" -> (InvalidAssessmentPrimaryCategoryConditionDescriptionCode, InvalidAssessmentPrimaryCategoryConditionDescription),
    "/assessments/condition/conditionTraderText"  -> (InvalidAssessmentPrimaryCategoryConditionTraderTextCode, InvalidAssessmentPrimaryCategoryConditionTraderText),
    "/supplementaryUnit"                          -> (InvalidOrMissingSupplementaryUnitCode, InvalidOrMissingSupplementaryUnit),
    "/measurementUnit"                            -> (InvalidOrMissingMeasurementUnitCode, InvalidOrMissingMeasurementUnit),
    "/comcodeEffectiveFromDate"                   -> (InvalidOrMissingComcodeEffectiveFromDateCode, InvalidOrMissingComcodeEffectiveFromDate),
    "/comcodeEffectiveToDate"                     -> (InvalidOrMissingComcodeEffectiveToDateCode, InvalidOrMissingComcodeEffectiveToDate),
    "/requestorName"                              -> (InvalidMissingRequestorNameCode, InvalidOrMissingRequestorName),
    "/requestorEmail"                             -> (InvalidMissingRequestorEmailCode, InvalidOrMissingRequestorEmail),
    "/ukimsNumber"                                -> (InvalidOrMissingUkimsNumberCode, InvalidOrMissingUkimsNumberMessage),
    "/nirmsNumber"                                -> (InvalidOrMissingNirmsNumberCode, InvalidOrMissingNirmsNumberMessage),
    "/niphlNumber"                                -> (InvalidOrMissingNiphlNumberCode, InvalidOrMissingNiphlNumberMessage)
  )

  val optionalFieldsToErrorCode: Map[String, (String, String)] = Map(
    "/eori"                                       -> (InvalidOrMissingEoriCode, InvalidOrMissingEori),
    "/recordId"                                   -> (RecordIdDoesNotExistsCode, InvalidRecordId),
    "/actorId"                                    -> (InvalidOrMissingActorIdCode, InvalidOrMissingActorId),
    "/traderRef"                                  -> (InvalidOrMissingTraderRefCode, InvalidOrMissingOptionalTraderRef),
    "/comcode"                                    -> (InvalidOrMissingComcodeCode, InvalidOrMissingOptionalComcode),
    "/goodsDescription"                           -> (InvalidOrMissingGoodsDescriptionCode, InvalidOrMissingOptionalGoodsDescription),
    "/countryOfOrigin"                            -> (InvalidOrMissingCountryOfOriginCode, InvalidOrMissingOptionalCountryOfOrigin),
    "/category"                                   -> (InvalidOrMissingCategoryCode, InvalidOrMissingOptionalCategory),
    "/assessments/assessmentId"                   -> (InvalidOrMissingAssessmentIdCode, InvalidOrMissingAssessmentId),
    "/assessments/primaryCategory"                -> (InvalidAssessmentPrimaryCategoryCode, InvalidAssessmentPrimaryCategory),
    "/assessments/condition/type"                 -> (InvalidAssessmentPrimaryCategoryConditionTypeCode, InvalidAssessmentPrimaryCategoryConditionType),
    "/assessments/condition/conditionId"          -> (InvalidAssessmentPrimaryCategoryConditionIdCode, InvalidAssessmentPrimaryCategoryConditionId),
    "/assessments/condition/conditionDescription" -> (InvalidAssessmentPrimaryCategoryConditionDescriptionCode, InvalidAssessmentPrimaryCategoryConditionDescription),
    "/assessments/condition/conditionTraderText"  -> (InvalidAssessmentPrimaryCategoryConditionTraderTextCode, InvalidAssessmentPrimaryCategoryConditionTraderText),
    "/supplementaryUnit"                          -> (InvalidOrMissingSupplementaryUnitCode, InvalidOrMissingSupplementaryUnit),
    "/measurementUnit"                            -> (InvalidOrMissingMeasurementUnitCode, InvalidOrMissingMeasurementUnit),
    "/comcodeEffectiveFromDate"                   -> (InvalidOrMissingComcodeEffectiveFromDateCode, InvalidOrMissingOptionalComcodeEffectiveFromDate),
    "/comcodeEffectiveToDate"                     -> (InvalidOrMissingComcodeEffectiveToDateCode, InvalidOrMissingComcodeEffectiveToDate),
    "/requestorName"                              -> (InvalidOrMissingRequestorNameCode, InvalidOrMissingRequestorName),
    "/requestorEmail"                             -> (InvalidOrMissingRequestorEmailCode, InvalidOrMissingRequestorEmail)
  )

  case class BadRequestErrorResponse(correlationId: String, errors: Seq[Error]) {
    def asPresentation =
      BadRequest(
        toJson(
          ErrorResponse(
            correlationId,
            BadRequestCode,
            BadRequestMessage,
            Some(errors)
          )
        )
      )
  }

  object BadRequestErrorResponse {
    implicit val format: OFormat[BadRequestErrorResponse] = Json.format[BadRequestErrorResponse]
  }
}
