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

package uk.gov.hmrc.tradergoodsprofilesrouter.connectors

import com.google.inject.Inject
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.StatusHttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.Error.{invalidRequestParameterError, unexpectedError}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.DownloadTraderDataConstants.{EoriDoesNotExistsCode, EoriDoesNotExistsMsg, EoriIsNotLinkedToAnyMsg, EoriIsNotLinkedToAnyRecord, InvalidCorrelationHeaderErrorCode, InvalidCorrelationHeaderErrorMsg, InvalidDateHeaderErrorCode, InvalidDateHeaderErrorMsg, InvalidForwardedHostCode, InvalidForwardedHostMsg, InvalidOrMissingEoriCode, InvalidOrMissingEoriMsg}

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
    val url = url"${appConfig.pegaConfig.downloadTraderDataUrl}/$eori/download"
    httpClientV2
      .get(url)
      .setHeader(
        commonHeaders(
          correlationId,
          appConfig.pegaConfig.downloadTraderDataBearerToken,
          appConfig.pegaConfig.forwardedHost
        ): _*
      )
      .execute(StatusHttpReader(correlationId, handleErrorResponse), ec)
  }

  override def parseFaultDetail(rawDetail: String, correlationId: String): Option[errors.Error] = {
    val regex = """error:\s*(\w+),\s*message:\s*(.*)""".r
    regex
      .findFirstMatchIn(rawDetail)
      .map(_ group 1)
      .collect {
        case InvalidCorrelationHeaderErrorCode =>
          invalidRequestParameterError(InvalidCorrelationHeaderErrorMsg, 1)
        case InvalidDateHeaderErrorCode        =>
          invalidRequestParameterError(InvalidDateHeaderErrorMsg, 2)
        case InvalidForwardedHostCode          =>
          invalidRequestParameterError(InvalidForwardedHostMsg, 5)
        case InvalidOrMissingEoriCode          =>
          invalidRequestParameterError(InvalidOrMissingEoriMsg, 6)
        case EoriDoesNotExistsCode             =>
          invalidRequestParameterError(EoriDoesNotExistsMsg, 7)
        case EoriIsNotLinkedToAnyRecord        =>
          invalidRequestParameterError(EoriIsNotLinkedToAnyMsg, 37)
        case other                             =>
          logger.warn(s"[DownloadTraderDataConnector] - Error code $other is not supported")
          unexpectedError("Unrecognised error number", other.toInt)
      }
  }
}
