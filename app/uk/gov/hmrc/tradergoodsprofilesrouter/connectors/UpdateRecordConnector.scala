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
import com.google.inject.Inject
import play.api.libs.json.Json.toJson
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.HttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.metrics.MetricsSupport
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.payloads.UpdateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService

import scala.concurrent.{ExecutionContext, Future}

class UpdateRecordConnector @Inject() (
  override val appConfig: AppConfig,
  httpClientV2: HttpClientV2,
  override val dateTimeService: DateTimeService,
  override val metricsRegistry: MetricRegistry
)(implicit val ec: ExecutionContext)
    extends BaseConnector
    with MetricsSupport
    with EisHttpErrorHandler {

  def updateRecord(
    payload: UpdateRecordPayload,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, CreateOrUpdateRecordResponse]] =
    withMetricsTimerAsync("tgp.updaterecord.connector") { _ =>
      val url = appConfig.eisConfig.updateRecordUrl

      httpClientV2
        .put(url"$url")
        .setHeader(buildHeaders(correlationId, appConfig.eisConfig.updateRecordBearerToken): _*)
        .withBody(toJson(payload))
        .execute(HttpReader[CreateOrUpdateRecordResponse](correlationId, handleErrorResponse), ec)
    }
}
