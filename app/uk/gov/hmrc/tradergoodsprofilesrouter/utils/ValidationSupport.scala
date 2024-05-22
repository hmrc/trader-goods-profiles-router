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

import play.api.libs.functional.syntax.toApplicativeOps
import play.api.libs.json.Reads.{maxLength, minLength}
import play.api.libs.json.{JsPath, JsonValidationError, Reads}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.Error
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._

import java.text.SimpleDateFormat
import java.util.{Locale, TimeZone}
import scala.util.Try

object ValidationSupport {

  private val dateFormat = generateDateFormat()

  def isValidCountryCode(rawCountryCode: String): Boolean =
    Locale.getISOCountries.toSeq.contains(rawCountryCode.toUpperCase)

  def isValidDate(rawDate: String): Boolean = Try(dateFormat.parse(rawDate)).isSuccess

  def convertError(
    errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]
  ): Seq[Error] =
    extractSimplePaths(errors)
      .map(key => fieldsToErrorCode.get(key).map(res => Error.invalidRequestParameterError(res._2, res._1.toInt)))
      .toSeq
      .flatten

  object Reads {
    def lengthBetween(min: Int, max: Int): Reads[String] =
      minLength[String](min).keepAnd(maxLength[String](max))
  }

  private def generateDateFormat(): SimpleDateFormat = {
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    dateFormat.setLenient(false)
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
    dateFormat
  }

  private def extractSimplePaths(
    errors: scala.collection.Seq[(JsPath, collection.Seq[JsonValidationError])]
  ): collection.Seq[String] =
    errors
      .map(_._1)
      .map(_.path)
      .map(_.mkString)

  private val fieldsToErrorCode: Map[String, (String, String)] = Map(
    "/eori"                                                       -> (InvalidOrMissingEoriCode, InvalidOrMissingEori),
    "/recordId"                                                   -> (RecordIdDoesNotExistsCode, InvalidRecordId),
    "/actorId"                                                    -> (InvalidOrMissingActorIdCode, InvalidOrMissingActorId),
    "/traderRef"                                                  -> (InvalidOrMissingTraderRefCode, InvalidOrMissingTraderRef),
    "/comcode"                                                    -> (InvalidOrMissingComcodeCode, InvalidOrMissingComcode),
    "/goodsDescription"                                           -> (InvalidOrMissingGoodsDescriptionCode, InvalidOrMissingGoodsDescription),
    "/countryOfOrigin"                                            -> (InvalidOrMissingCountryOfOriginCode, InvalidOrMissingCountryOfOrigin),
    "/category"                                                   -> (InvalidOrMissingCategoryCode, InvalidOrMissingCategory),
    "/assessments"                                                -> (InvalidOrMissingAssessmentIdCode, InvalidOrMissingAssessmentId),
    "/supplementaryUnit"                                          -> (InvalidAssessmentPrimaryCategoryCode, InvalidAssessmentPrimaryCategory),
    "/assessments/primaryCategory/condition/type"                 -> (InvalidAssessmentPrimaryCategoryConditionTypeCode, InvalidAssessmentPrimaryCategoryConditionType),
    "/assessments/primaryCategory/condition/conditionId"          -> (InvalidAssessmentPrimaryCategoryConditionIdCode, InvalidAssessmentPrimaryCategoryConditionId),
    "/assessments/primaryCategory/condition/conditionDescription" -> (InvalidAssessmentPrimaryCategoryConditionDescriptionCode, InvalidAssessmentPrimaryCategoryConditionDescription),
    "/assessments/primaryCategory/condition/conditionTraderText"  -> (InvalidAssessmentPrimaryCategoryConditionTraderTextCode, InvalidAssessmentPrimaryCategoryConditionTraderText),
    "/supplementaryUnit"                                          -> (InvalidOrMissingSupplementaryUnitCode, InvalidOrMissingSupplementaryUnit),
    "/measurementUnit"                                            -> (InvalidOrMissingMeasurementUnitCode, InvalidOrMissingMeasurementUnit),
    "/comcodeEffectiveFromDate"                                   -> (InvalidOrMissingComcodeEffectiveFromDateCode, InvalidOrMissingComcodeEffectiveFromDate),
    "/comcodeEffectiveToDate"                                     -> (InvalidOrMissingComcodeEffectiveToDateCode, InvalidOrMissingComcodeEffectiveToDate)
  )
}
