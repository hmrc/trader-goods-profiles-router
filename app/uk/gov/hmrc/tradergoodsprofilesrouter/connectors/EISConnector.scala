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
import play.api.mvc.Result
import play.api.libs.json.Json.toJson
import sttp.model.Uri.UriContext
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
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
    request: CreateRecordRequest,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[CreateRecordResponse]

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
    ec: ExecutionContext,
    hc: HeaderCarrier,
    correlationId: String
  ): Future[Either[Result, GetEisRecordsResponse]] = {
    val url = s"${appConfig.eisConfig.url}/$eori/$recordId"

    httpClientV2
      .get(url"$url")(hc)
      .setHeader(eisRequestHeaders(correlationId): _*)
      .executeAndDeserialise[GetEisRecordsResponse]
  }

  override def fetchRecords(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier,
    correlationId: String
  ): Future[Either[Result, GetEisRecordsResponse]] = {
    val uri =
      uri"${appConfig.eisConfig.getRecordsUrl}/$eori?lastUpdatedDate=$lastUpdatedDate&page=$page&size=$size"

    httpClientV2
      .get(url"$uri")(hc)
      .setHeader(eisRequestHeaders(correlationId): _*)
      .executeAndDeserialise[GetEisRecordsResponse]
  }

  override def createRecord(
    request: CreateRecordRequest,
    correlationId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[CreateRecordResponse] = {
    val url = appConfig.eisConfig.createRecordUrl

    httpClientV2
      .post(url"$url")(hc)
      .setHeader(eisRequestHeaders(correlationId): _*)
      .withBody(toJson(request))
      .executeAndDeserialise[CreateRecordResponse]
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
