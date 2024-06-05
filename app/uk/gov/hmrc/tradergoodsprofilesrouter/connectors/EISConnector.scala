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
import play.api.http.MimeTypes
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import sttp.model.Uri.UriContext
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.{HttpReader, OtherHttpReader}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.RemoveEisRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.accreditationrequests.{RequestEisAccreditationRequest, TraderDetails}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.{CreateRecordRequest, UpdateRecordRequest}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.{HttpReader, RemoveRecordHttpReader}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.{MaintainProfileEisRequest, RemoveEisRecordRequest}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.{CreateRecordRequest, UpdateRecordRequest}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{GetEisRecordsResponse, MaintainProfileResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames._

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EISConnector @Inject() (
  appConfig: AppConfig,
  httpClientV2: HttpClientV2,
  dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext)
    extends EisHttpErrorHandler {

  def fetchRecord(
    eori: String,
    recordId: String,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, GetEisRecordsResponse]] = {
    val url = s"${appConfig.eisConfig.getRecordsUrl}/$eori/$recordId"

    httpClientV2
      .get(url"$url")
      .setHeader(eisRequestHeaders(correlationId, appConfig.eisConfig.getRecordBearerToken): _*)
      .execute(HttpReader[GetEisRecordsResponse](correlationId, handleErrorResponse), ec)

  }

  def fetchRecords(
    eori: String,
    correlationId: String,
    lastUpdatedDate: Option[Instant] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit hc: HeaderCarrier): Future[Either[Result, GetEisRecordsResponse]] = {

    val formattedLastUpdateDate: Option[String] = lastUpdatedDate.map(_.asStringSeconds)
    val uri                                     =
      uri"${appConfig.eisConfig.getRecordsUrl}/$eori?lastUpdatedDate=$formattedLastUpdateDate&page=$page&size=$size"

    httpClientV2
      .get(url"$uri")
      .setHeader(eisRequestHeaders(correlationId, appConfig.eisConfig.getRecordBearerToken): _*)
      .execute(HttpReader[GetEisRecordsResponse](correlationId, handleErrorResponse), ec)
  }

  def createRecord(
    request: CreateRecordRequest,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, CreateOrUpdateRecordResponse]] = {
    val url = appConfig.eisConfig.createRecordUrl

    httpClientV2
      .post(url"$url")
      .setHeader(eisRequestHeaders(correlationId, appConfig.eisConfig.createRecordBearerToken): _*)
      .withBody(toJson(request))
      .execute(HttpReader[CreateOrUpdateRecordResponse](correlationId, handleErrorResponse), ec)
  }

  def updateRecord(
    request: UpdateRecordRequest,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, CreateOrUpdateRecordResponse]] = {
    val url = appConfig.eisConfig.updateRecordUrl

    httpClientV2
      .put(url"$url")
      .setHeader(eisRequestHeaders(correlationId, appConfig.eisConfig.updateRecordBearerToken): _*)
      .withBody(toJson(request))
      .execute(HttpReader[CreateOrUpdateRecordResponse](correlationId, handleErrorResponse), ec)
  }

  override def requestAccreditation(request: TraderDetails, correlationId: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[Result, Int]] = {
    val url = appConfig.eisConfig.createaccreditationUrl

    val accreditationEisRequest = RequestEisAccreditationRequest(request, dateTimeService.timestamp.asStringHttp)
    httpClientV2
      .post(url"$url")
      .setHeader(eisRequestHeadersAccreditation(correlationId, appConfig.eisConfig.createAccreditationBearerToken): _*)
      .withBody(toJson(accreditationEisRequest))
      .execute(OtherHttpReader[Int](correlationId, handleErrorResponse), ec)
  }

  override def removeRecord(
    eori: String,
    recordId: String,
    actorId: String,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, Int]] = {
    val url = appConfig.eisConfig.removeRecordUrl
    httpClientV2
      .put(url"$url")
      .setHeader(eisRequestHeaders(correlationId, appConfig.eisConfig.removeRecordBearerToken): _*)
      .withBody(toJson(RemoveEisRecordRequest(eori, recordId, actorId)))
      .execute(OtherHttpReader[Int](correlationId, handleErrorResponse), ec)
  }

  def maintainProfile(request: MaintainProfileEisRequest, correlationId: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[Result, MaintainProfileResponse]] = {
    val url = appConfig.eisConfig.maintainProfileUrl
    httpClientV2
      .put(url"$url")
      .setHeader(eisRequestHeaders(correlationId): _*)
      .withBody(toJson(request))
      .execute(HttpReader[MaintainProfileResponse](correlationId, handleErrorResponse), ec)
  }

  private def eisRequestHeaders(correlationId: String, bearerToken: String)(implicit
    hc: HeaderCarrier
  ): Seq[(String, String)] =
    Seq(
      CorrelationId -> correlationId,
      ForwardedHost -> appConfig.eisConfig.forwardedHost,
      ContentType   -> MimeTypes.JSON,
      Accept        -> MimeTypes.JSON,
      Date          -> dateTimeService.timestamp.asStringHttp,
      ClientId      -> hc.headers(Seq(ClientId)).head._2,
      Authorization -> bearerToken
    )

  private def eisRequestHeadersAccreditation(correlationId: String, bearerToken: String)(implicit
    hc: HeaderCarrier
  ): Seq[(String, String)] =
    eisRequestHeaders(correlationId, bearerToken).filterNot(elm =>
      elm == HeaderNames.ClientId -> hc.headers(Seq(HeaderNames.ClientId)).head._2
    )
}
