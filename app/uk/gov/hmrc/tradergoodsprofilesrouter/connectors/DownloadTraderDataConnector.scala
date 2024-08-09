package uk.gov.hmrc.tradergoodsprofilesrouter.connectors

import com.google.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.StatusHttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService

import scala.concurrent.{ExecutionContext, Future}

class DownloadTraderDataConnector @Inject() (
  override val appConfig: AppConfig,
  httpClientV2: HttpClientV2,
  override val dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext)
    extends BaseConnector
    with EisHttpErrorHandler {

  def requestDownload(eori: String, correlationId: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[EisHttpErrorResponse, Int]] = {
    val url = appConfig.pegaConfig.downloadTraderDataUrl
    httpClientV2
      .get(url"$url")
      .setHeader(
        buildHeaders(
          correlationId,
          appConfig.pegaConfig.downloadTraderDataBearerToken,
          appConfig.pegaConfig.forwardedHost
        ): _*
      )
      .execute(StatusHttpReader(correlationId, handleErrorResponse), ec)
  }
}
