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
import play.api.libs.json.Json.toJson
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.HttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.CreateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordEisResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.payloads.UpdateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import scala.concurrent.{ExecutionContext, Future}

class UpdateRecordConnector @Inject() (
  override val appConfig: AppConfig,
  httpClientV2: HttpClientV2,
  override val dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext)
    extends BaseConnector
    with EisHttpErrorHandler {

  def updateRecord(
    payload: UpdateRecordPayload,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, CreateOrUpdateRecordEisResponse]] = {
    val url = appConfig.hawkConfig.updateRecordUrl

    httpClientV2
      .put(url"$url")
      .setHeader(
        buildHeadersWithDrop1Toggle(
          correlationId,
          appConfig.hawkConfig.updateRecordBearerToken,
          appConfig.hawkConfig.forwardedHost
        ): _*
      )
      .withBody(toJson(payload))
      .execute(HttpReader[CreateOrUpdateRecordEisResponse](correlationId, handleErrorResponse), ec)
  }

  def put(
    payload: CreateRecordPayload,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, CreateOrUpdateRecordEisResponse]] = {
    val url = appConfig.hawkConfig.updateRecordUrl

    httpClientV2
      .put(url"$url")
      .setHeader(
        commonHeaders(
          correlationId,
          appConfig.hawkConfig.updateRecordBearerToken,
          appConfig.hawkConfig.forwardedHost
        ) ++ Seq(
          HeaderNames.Accept      -> MimeTypes.JSON,
          HeaderNames.ContentType -> MimeTypes.JSON
        ): _*
      )
      .withBody(toJson(payload))
      .execute(HttpReader[CreateOrUpdateRecordEisResponse](correlationId, handleErrorResponse), ec)
  }
}
