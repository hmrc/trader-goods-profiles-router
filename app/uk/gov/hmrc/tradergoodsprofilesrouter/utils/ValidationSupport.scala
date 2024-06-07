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

import org.apache.commons.validator.routines.EmailValidator
import play.api.libs.functional.syntax.toApplicativeOps
import play.api.libs.json.Reads.{maxLength, minLength, verifying}
import play.api.libs.json.{JsPath, JsonValidationError, KeyPathNode, Reads}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.UpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.Error
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._

import java.time.Instant
import java.util.Locale
import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.matching.Regex

object ValidationSupport {

  private val actorIdPattern: Regex = raw"[A-Z]{2}\d{12,15}".r
  private val comcodePattern: Regex = raw".{6}(.{2}(.{2})?)?".r

  def isValidCountryCode(rawCountryCode: String): Boolean =
    Locale.getISOCountries.toSeq.contains(rawCountryCode.toUpperCase)

  def isValidDate(instant: Instant): Boolean =
    instant.getNano == 0

  private val emailValidator: EmailValidator = EmailValidator.getInstance(true)

  def convertError1[T](
    errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])],
    fieldToErrorCodeTable: Map[String, (String, String)]
  ): Seq[Error] =
    extractSimplePaths(errors)
      .map(key => fieldToErrorCodeTable.get(key).map(res => Error.invalidRequestParameterError(res._2, res._1.toInt)))
      .toSeq
      .flatten

  def convertError[T](
    errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]
  )(implicit tt: TypeTag[T]): Seq[Error] =
    extractSimplePaths(errors)
      .map(key =>
        tt.tpe match {
          case t if t =:= typeOf[UpdateRecordRequest] =>
            optionalFieldsToErrorCode.get(key).map(res => Error.invalidRequestParameterError(res._2, res._1.toInt))
          case _                                      =>
            fieldsToErrorCode.get(key).map(res => Error.invalidRequestParameterError(res._2, res._1.toInt))
        }
      )
      .toSeq
      .flatten

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
