package uk.gov.hmrc.tradergoodsprofilesrouter.service

import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{DownloadTraderDataConnector, EisHttpErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import scala.concurrent.{ExecutionContext, Future}

class DownloadTraderDataService @Inject() (
  connector: DownloadTraderDataConnector,
  uuidService: UuidService,
)(implicit ec: ExecutionContext)
    extends Logging {

  def requestDownload(eori: String)(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, Int]] = {
    val correlationId = uuidService.uuid
    connector
      .requestDownload(eori, correlationId)
      .map {
        case Right(_)       => Right(ACCEPTED)
        case Left(response) => Left(response)
      } recover { case e: Throwable =>
      logger.error(
        s"""[DownloadTraderDataService] - Error occurred while requesting download of trader data for Eori: $eori correlationId: $correlationId, message: ${e.getMessage}""",
        e
      )
      Left(EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, UnexpectedErrorCode, e.getMessage)))
    }
  }
}
