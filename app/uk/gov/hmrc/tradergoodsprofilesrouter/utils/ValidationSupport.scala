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

import java.text.SimpleDateFormat
import java.util.{Locale, TimeZone}
import scala.util.Try

object ValidationSupport {

  private val dateFormat = generateDateFormat()

  def isValidCountryCode(rawCountryCode: String): Boolean =
    Locale.getISOCountries.toSeq.contains(rawCountryCode.toUpperCase)

  def isValidDate(rawDate: String): Boolean = Try(dateFormat.parse(rawDate)).isSuccess

  def convertError(
    errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])],
    fieldsToErrorCode: Map[String, (String, String)]
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
}
