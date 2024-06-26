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

import com.codahale.metrics.MetricRegistry
import sttp.model.Uri.UriContext
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.HttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.metrics.MetricsSupport
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetRecordsConnector @Inject() (
  override val appConfig: AppConfig,
  httpClientV2: HttpClientV2,
  override val dateTimeService: DateTimeService,
  override val metricsRegistry: MetricRegistry
)(implicit val ec: ExecutionContext)
    extends BaseConnector
    with MetricsSupport
    with EisHttpErrorHandler {

  def fetchRecord(
    eori: String,
    recordId: String,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, GetEisRecordsResponse]] =
    withMetricsTimerAsync("tgp.getrecord.connector") { _ =>
      val url = s"${appConfig.eisConfig.getRecordsUrl}/$eori/$recordId"

      httpClientV2
        .get(url"$url")
        .setHeader(buildHeadersForGetMethod(correlationId, appConfig.eisConfig.getRecordBearerToken): _*)
        .execute(HttpReader[GetEisRecordsResponse](correlationId, handleErrorResponse), ec)
    }

  def fetchRecords(
    eori: String,
    correlationId: String,
    lastUpdatedDate: Option[Instant] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, GetEisRecordsResponse]] =
    withMetricsTimerAsync("tgp.getrecords.connector") { _ =>
      val formattedLastUpdateDate: Option[String] = lastUpdatedDate.map(_.asStringSeconds)
      val uri                                     =
        uri"${appConfig.eisConfig.getRecordsUrl}/$eori?lastUpdatedDate=$formattedLastUpdateDate&page=$page&size=$size"

      httpClientV2
        .get(url"$uri")
        .setHeader(buildHeadersForGetMethod(correlationId, appConfig.eisConfig.getRecordBearerToken): _*)
        .execute(HttpReader[GetEisRecordsResponse](correlationId, handleErrorResponse), ec)
    }

}
