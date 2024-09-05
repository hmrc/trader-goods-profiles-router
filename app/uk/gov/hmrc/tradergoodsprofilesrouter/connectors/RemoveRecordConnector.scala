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
import play.api.http.MimeTypes
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.StatusHttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.RemoveEisRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import scala.concurrent.{ExecutionContext, Future}

class RemoveRecordConnector @Inject() (
  override val appConfig: AppConfig,
  httpClientV2: HttpClientV2,
  override val dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext)
    extends BaseConnector
    with EisHttpErrorHandler {

  def removeRecord(
    eori: String,
    recordId: String,
    actorId: String,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, Int]] = {
    val url = appConfig.hawkConfig.removeRecordUrl
    httpClientV2
      .put(url"$url")
      .setHeader(
        headers(
          correlationId,
          appConfig.hawkConfig.removeRecordBearerToken,
          appConfig.hawkConfig.forwardedHost
        ): _*
      )
      .withBody(Json.toJson(RemoveEisRecordRequest(eori, recordId, actorId)))
      .execute(StatusHttpReader(correlationId, handleErrorResponse), ec)
  }

  /*
  ToDo: remove isDrop2Enabled flag after drop2 - TGP-2029.
   The header passed to EIS should have no Accept and ClientId header
   */
  private def headers(correlationId: String, accessToken: String, forwardedHost: String)(implicit
    hc: HeaderCarrier
  ): Seq[(String, String)] = {
    val headers      = commonHeaders(correlationId, accessToken, forwardedHost)
    val extraHeaders = if (appConfig.isDrop2Enabled) {

      Seq(HeaderNames.ContentType -> MimeTypes.JSON)
    } else if (appConfig.acceptHeaderDisabled) {
      Seq(HeaderNames.ClientId -> getClientId)
    } else if (appConfig.isContentTypeHeaderDisabled) {
      Seq(HeaderNames.Accept -> MimeTypes.JSON, HeaderNames.ClientId -> getClientId)
    } else
      Seq(
        HeaderNames.Accept      -> MimeTypes.JSON,
        HeaderNames.ContentType -> MimeTypes.JSON,
        HeaderNames.ClientId    -> getClientId
      )
    headers ++ extraHeaders
  }
}
