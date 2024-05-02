package uk.gov.hmrc.tradergoodsprofilesrouter.models.response

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.SourceFaultDetail

import java.time.Instant

case class ErrorResponse(
                        code: String,
                        message: String,
                        errors: Option[Seq[Error]] = None
                      )

case class Error(code: String, message: String)

object ErrorResponse {
  implicit val format: OFormat[ErrorResponse] = Json.format[ErrorResponse]
}

object Error {
  implicit val format: OFormat[Error] = Json.format[Error]
}
