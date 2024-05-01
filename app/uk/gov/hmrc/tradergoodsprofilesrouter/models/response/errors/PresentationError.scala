/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors

import cats.data.NonEmptyList
import play.api.http.Status.BAD_REQUEST
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.{Json, OFormat, OWrites, Reads, __}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.format.CommonFormats

object PresentationError extends CommonFormats {

  val MessageFieldName = "message"
  val CodeFieldName    = "code"

  def forbiddenError(message: String): PresentationError =
    StandardError(message, ErrorCode.Forbidden)

  def unsupportedMediaTypeError(message: String): PresentationError =
    StandardError(message, ErrorCode.UnsupportedMediaType)

  def notAcceptableError(message: String): PresentationError =
    StandardError(message, ErrorCode.NotAcceptable)

  def badRequestError(message: String): PresentationError =
    StandardError(message, ErrorCode.BadRequest)

  def bindingBadRequestError(message: String): PresentationError =
    BindingError(message, BAD_REQUEST, ErrorCode.BadRequest)

  def notFoundError(message: String): PresentationError =
    StandardError(message, ErrorCode.NotFound)

  def businessValidationError(message: String): PresentationError =
    StandardError(message, ErrorCode.BusinessValidation)

  def unauthorized(message: String): PresentationError =
    StandardError(message, ErrorCode.Unauthorized)

  def jsonSchemaValidationError(
    message: String = "Request failed schema validation",
    validationErrors: NonEmptyList[JsonValidationError]
  ): JsonSchemaValidationError =
    JsonSchemaValidationError(message, ErrorCode.SchemaValidation, validationErrors)

  def internalServiceError(
    message: String = "Internal server error",
    code: ErrorCode = ErrorCode.InternalServerError,
    cause: Option[Throwable] = None
  ): PresentationError =
    InternalServiceError(message, code, cause)

  def unapply(error: PresentationError): Option[(String, ErrorCode)] = Some((error.message, error.code))

  private val baseErrorWrites0: OWrites[PresentationError] =
    (
      (__ \ MessageFieldName).write[String] and
        (__ \ CodeFieldName).write[ErrorCode]
    )(unlift(PresentationError.unapply))

  implicit val standardErrorReads: Reads[StandardError] =
    (
      (__ \ MessageFieldName).read[String] and
        (__ \ CodeFieldName).read[ErrorCode]
    )(StandardError.apply _)

  implicit val invalidOfficeErrorReads: Reads[InvalidEisError] = Json.reads[InvalidEisError]

  implicit val jsonSchemaErrorFormat: OFormat[JsonSchemaValidationError] =
    (
      (__ \ MessageFieldName).format[String] and
        (__ \ CodeFieldName).format[ErrorCode] and
        (__ \ "validationErrors").format[NonEmptyList[JsonValidationError]]
    )(JsonSchemaValidationError.apply, unlift(JsonSchemaValidationError.unapply))

  implicit val baseErrorWrites: OWrites[PresentationError] = OWrites {
    case bindingError: BindingError                           => Json.writes[BindingError].writes(bindingError)
    case jsonSchemaValidationError: JsonSchemaValidationError => jsonSchemaErrorFormat.writes(jsonSchemaValidationError)
    case baseError                                            => baseErrorWrites0.writes(baseError)
  }

}

sealed abstract class PresentationError extends Product with Serializable {
  def message: String
  def code: ErrorCode
}

case class StandardError(message: String, code: ErrorCode) extends PresentationError

case class InvalidEisError(message: String, eori: String, field: String, code: ErrorCode) extends PresentationError

case class BindingError(message: String, statusCode: Int, code: ErrorCode) extends PresentationError

case class JsonSchemaValidationError(
  message: String,
  code: ErrorCode,
  validationErrors: NonEmptyList[JsonValidationError]
) extends PresentationError

case class UpstreamServiceError(
  message: String = "Internal server error",
  code: ErrorCode = ErrorCode.InternalServerError,
  cause: UpstreamErrorResponse
) extends PresentationError

case class InternalServiceError(
  message: String = "Internal server error",
  code: ErrorCode = ErrorCode.InternalServerError,
  cause: Option[Throwable] = None
) extends PresentationError

object InternalServiceError {

  def causedBy(cause: Throwable): PresentationError =
    PresentationError.internalServiceError(cause = Some(cause))
}
