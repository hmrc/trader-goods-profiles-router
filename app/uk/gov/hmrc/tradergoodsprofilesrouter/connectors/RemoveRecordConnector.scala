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
import play.api.mvc.Result
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.OtherHttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.RemoveEisRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames._

import scala.concurrent.{ExecutionContext, Future}

class RemoveRecordConnector @Inject() (
  appConfig: AppConfig,
  httpClientV2: HttpClientV2,
  dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext)
    extends EisHttpErrorHandler {

  def removeRecord(
    eori: String,
    recordId: String,
    actorId: String,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, Int]] = {
    val url = appConfig.eisConfig.removeRecordUrl
    httpClientV2
      .put(url"$url")
      .setHeader(eisRequestHeaders(correlationId, appConfig.eisConfig.removeRecordBearerToken): _*)
      .withBody(Json.toJson(RemoveEisRecordRequest(eori, recordId, actorId)))
      .execute(OtherHttpReader[Int](correlationId, handleErrorResponse), ec)
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
}
