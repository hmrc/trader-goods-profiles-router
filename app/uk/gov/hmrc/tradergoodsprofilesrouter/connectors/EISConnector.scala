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
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[EISConnectorImpl])
trait EISConnector {
  def fetchRecord(
    eori: String,
    recordId: String,
    correlationId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[GetEisRecordsResponse]
}

class EISConnectorImpl @Inject() (
  appConfig: AppConfig,
  httpClientV2: HttpClientV2,
  dateTimeService: DateTimeService
) extends EISConnector
    with BaseConnector {

  override def fetchRecord(
    eori: String,
    recordId: String,
    correlationId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[GetEisRecordsResponse] = {
    val url     = s"${appConfig.eisConfig.url}/$eori/$recordId"
    val headers = Seq(
      HeaderNames.CORRELATION_ID -> correlationId,
      HeaderNames.FORWARDED_HOST -> "MDTP",
      HeaderNames.CONTENT_TYPE   -> MimeTypes.JSON,
      HeaderNames.ACCEPT         -> MimeTypes.JSON,
      HeaderNames.DATE           -> dateTimeService.timestamp.toString,
      HeaderNames.CLIENT_ID      -> hc.headers(Seq("X-Client-ID")).headOption.map(_._2).getOrElse(""),
      HeaderNames.AUTHORIZATION  -> appConfig.eisConfig.headers.authorization
    )

    httpClientV2
      .get(url"$url")(hc)
      .setHeader(headers: _*)
      .executeAndDeserialise[GetEisRecordsResponse]
  }
}
