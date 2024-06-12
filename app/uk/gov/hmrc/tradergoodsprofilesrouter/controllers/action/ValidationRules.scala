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

import cats.data.EitherT
import cats.implicits.catsSyntaxTuple2Parallel
import cats.syntax.all._
import org.apache.commons.validator.routines.EmailValidator
import play.api.libs.functional.syntax.toApplicativeOps
import play.api.libs.json.Reads.{maxLength, minLength, verifying}
import play.api.libs.json._
import play.api.mvc.{BaseController, Request, Result}
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.{ValidatedQueryParameters, extractSimplePaths, isValidActorId}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{BadRequestErrorResponse, Error}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import java.time.Instant
import java.util.{Locale, UUID}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.matching.Regex

//todo: we may want to unify ValidationSupport and this one.
trait ValidationRules {
  this: BaseController =>

  def uuidService: UuidService

  implicit def ec: ExecutionContext

  protected def validateClientId(implicit request: Request[_]): EitherT[Future, Result, String] =
    EitherT
      .fromOption[Future](
        request.headers.get(HeaderNames.ClientId),
        Error(InvalidHeader, MissingHeaderClientId, 6000)
      )
      .leftMap(e => BadRequestErrorResponse(uuidService.uuid, Seq(e)).asPresentation)

  protected def validateRecordId(recordId: String): Either[Error, String] =
    Try(UUID.fromString(recordId).toString).toOption.toRight(
      Error(
        InvalidQueryParameter,
        InvalidRecordIdQueryParameter,
        InvalidRecordIdCode.toInt
      )
    )

  protected def validateActorId(actorId: String): Either[Error, String] =
    if (isValidActorId(actorId)) Right(actorId)
    else
      Left(
        Error(
          InvalidQueryParameter,
          InvalidActorIdQueryParameter,
          InvalidOrMissingActorIdCode.toInt
        )
      )

  protected def validateRequestBody[A: Reads](
    fieldToErrorCodeTable: Map[String, (String, String)]
  )(implicit request: Request[JsValue]): EitherT[Future, Result, A] =
    EitherT
      .fromEither[Future](
        request.body
          .validate[A]
          .asEither
      )
      .leftMap { errors =>
        BadRequestErrorResponse(
          uuidService.uuid,
          convertError[A](errors, fieldToErrorCodeTable)
        ).asPresentation
      }

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

  val actorIdPattern: Regex = raw"[A-Z]{2}\d{12,15}".r
  val comcodePattern: Regex = raw".{6}(.{2}(.{2})?)?".r

  def isValidCountryCode(rawCountryCode: String): Boolean =
    Locale.getISOCountries.toSeq.contains(rawCountryCode.toUpperCase)

  def isValidDate(instant: Instant): Boolean =
    instant.getNano == 0

  private val emailValidator: EmailValidator = EmailValidator.getInstance(true)

  object Reads {
    def lengthBetween(min: Int, max: Int): Reads[String] =
      minLength[String](min).keepAnd(maxLength[String](max))

    val validActorId: Reads[String] = verifying(isValidActorId)

    val validComcode: Reads[String] = verifying(isValidComcode)

    val validDate: Reads[Instant]        = verifying(isValidDate)
    val validEmailAddress: Reads[String] = verifying(isValidEmailAddress)
  }

  def isValidEmailAddress(emailAddress: String): Boolean = emailValidator.isValid(emailAddress)

  def isValidActorId(actorId: String): Boolean = actorIdPattern.matches(actorId)

  def isValidComcode(comcode: String): Boolean = comcodePattern.matches(comcode)

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
}
