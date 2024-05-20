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
import com.google.inject.ImplementedBy
import play.api.http.MimeTypes
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import sttp.model.Uri.UriContext
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.HttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[EISConnectorImpl])
trait EISConnector {
  def fetchRecord(
    eori: String,
    recordId: String
  )(implicit
    hc: HeaderCarrier,
    correlationId: String
  ): Future[Either[Result, GetEisRecordsResponse]]

  def fetchRecords(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit
    hc: HeaderCarrier,
    correlationId: String
  ): Future[Either[Result, GetEisRecordsResponse]]

  def createRecord(
    request: CreateRecordRequest
  )(implicit hc: HeaderCarrier, correlationId: String): Future[Either[Result, CreateRecordResponse]]

}

class EISConnectorImpl @Inject() (
  appConfig: AppConfig,
  httpClientV2: HttpClientV2,
  dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext)
    extends EISConnector
    with BaseConnector {

  override def fetchRecord(
    eori: String,
    recordId: String
  )(implicit
    hc: HeaderCarrier,
    correlationId: String
  ): Future[Either[Result, GetEisRecordsResponse]] = {
    val url = s"${appConfig.eisConfig.getRecordsUrl}/$eori/$recordId"

    httpClientV2
      .get(url"$url")
      .setHeader(eisRequestHeaders(correlationId): _*)
      .execute(HttpReader[GetEisRecordsResponse](correlationId), ec)

  }

  override def fetchRecords(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit
    hc: HeaderCarrier,
    correlationId: String
  ): Future[Either[Result, GetEisRecordsResponse]] = {
    val uri =
      uri"${appConfig.eisConfig.getRecordsUrl}/$eori?lastUpdatedDate=$lastUpdatedDate&page=$page&size=$size"

    httpClientV2
      .get(url"$uri")
      .setHeader(eisRequestHeaders(correlationId): _*)
      .execute(HttpReader[GetEisRecordsResponse](correlationId), ec)
  }

  override def createRecord(
    request: CreateRecordRequest
  )(implicit hc: HeaderCarrier, correlationId: String): Future[Either[Result, CreateRecordResponse]] = {
    val url = appConfig.eisConfig.createRecordUrl

    httpClientV2
      .post(url"$url")
      .setHeader(eisRequestHeaders(correlationId): _*)
      .withBody(toJson(request))
      .execute(HttpReader[CreateRecordResponse](correlationId), ec)

  }

  private def eisRequestHeaders(correlationId: String)(implicit hc: HeaderCarrier): Seq[(String, String)] =
    Seq(
      HeaderNames.CorrelationId -> correlationId,
      HeaderNames.ForwardedHost -> appConfig.eisConfig.forwardedHost,
      HeaderNames.ContentType   -> MimeTypes.JSON,
      HeaderNames.Accept        -> MimeTypes.JSON,
      HeaderNames.Date          -> dateTimeService.timestamp.asStringHttp,
      HeaderNames.ClientId      -> hc.headers(Seq(HeaderNames.ClientId)).head._2,
      HeaderNames.Authorization -> appConfig.eisConfig.headers.authorization
    )
}
