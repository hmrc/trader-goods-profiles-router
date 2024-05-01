package uk.gov.hmrc.tradergoodsprofilesrouter.service

import cats.data.EitherT
import com.google.inject.{ImplementedBy, Inject}
import play.api.http.Status.{BAD_REQUEST, CONFLICT}
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.eis.response.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.error.EisError

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

@ImplementedBy(classOf[RouterServiceImpl])
trait RouterService {
  def fetchRecord(
    eori: String,
    recordId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): EitherT[Future, EisError, GetEisRecordsResponse]
}

class RouterServiceImpl @Inject() (eisConnector: EISConnector) extends RouterService {
  override def fetchRecord(eori: String, recordId: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, EisError, GetEisRecordsResponse] =
    EitherT {
      eisConnector
        .fetchRecord(eori, recordId)
        .map(result => Right(result))
        .recover {
          case UpstreamErrorResponse(message, BAD_REQUEST, _, _) => Left(determineError(message))
          // case UpstreamErrorResponse(message, INTERNAL_SERVER_ERROR, _, _)    => Left(onConflict(message))
          case NonFatal(e)                                       =>
            //logger.error(s"Unable to send to EIS : ${e.getMessage}", e)
            Left(EisError.UnexpectedError(thr = Some(e)))
        }
    }

  private def determineError(message: String): EisError =
    Try(Json.parse(message))
      .map(_.validate[InvalidOfficeError])
      .map {
        case JsSuccess(value: InvalidOfficeError, _) => RouterError.UnrecognisedOffice(value.office, value.field)
        case _                                       => RouterError.UnexpectedError()
      }
      .getOrElse(EisError.UnexpectedError()) // we didn't get Json, but the exception here would then be a
  // red herring, so don't pass it on

}
