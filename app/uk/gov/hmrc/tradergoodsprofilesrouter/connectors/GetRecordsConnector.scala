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

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import sttp.model.Uri.UriContext
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.HttpReader
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
  override val actorSystem: ActorSystem,
  override val configuration: Config
)(implicit val ec: ExecutionContext)
    extends BaseConnector
    with EisHttpErrorHandler {

  def fetchRecord(
    eori: String,
    recordId: String,
    correlationId: String,
    urlPath: String
  )(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, GetEisRecordsResponse]] = {
    val url = s"$urlPath/$eori/$recordId"

    retryFor[GetEisRecordsResponse]("fetch record")(retryCondition) {
      httpClientV2
        .get(url"$url")
        .setHeader(
          buildHeadersForGetMethod(
            correlationId,
            appConfig.hawkConfig.getRecordBearerToken,
            appConfig.hawkConfig.forwardedHost
          ): _*
        )
        .execute(HttpReader[GetEisRecordsResponse](correlationId, handleErrorResponse), ec)
    }
  }

  def fetchRecords(
    eori: String,
    correlationId: String,
    size: Int,
    page: Option[Int] = None,
    lastUpdatedDate: Option[Instant] = None
  )(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, GetEisRecordsResponse]] = {
    val formattedLastUpdateDate: Option[String] = lastUpdatedDate.map(_.asStringSeconds)
    val uri                                     =
      uri"${appConfig.hawkConfig.getRecordsUrl}/$eori?lastUpdatedDate=$formattedLastUpdateDate&page=$page&size=$size"

    retryFor[GetEisRecordsResponse]("fetch records")(retryCondition) {
      httpClientV2
        .get(url"$uri")
        .setHeader(
          buildHeadersForGetMethod(
            correlationId,
            appConfig.hawkConfig.getRecordBearerToken,
            appConfig.hawkConfig.forwardedHost
          ): _*
        )
        .execute(HttpReader[GetEisRecordsResponse](correlationId, handleErrorResponse), ec)
    }
  }

}
