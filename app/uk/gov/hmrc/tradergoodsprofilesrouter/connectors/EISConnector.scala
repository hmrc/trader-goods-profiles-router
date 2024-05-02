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
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.EISInstanceConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait EISConnector {
  def fetchRecord(
    eori: String,
    recordId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[GetEisRecordsResponse]
}

class EISConnectorImpl(
  eisInstanceConfig: EISInstanceConfig,
  httpClientV2: HttpClientV2,
  dateTimeService: DateTimeService
) extends EISConnector
    with BaseConnector {

  override def fetchRecord(
    eori: String,
    recordId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[GetEisRecordsResponse] = {
    val url           = s"${eisInstanceConfig.url}/$eori/$recordId"
    val correlationId = UUID.randomUUID().toString
    val headers       = Seq(
      HeaderNames.CORRELATION_ID -> correlationId,
      HeaderNames.FORWARDED_HOST -> "localhost",
      HeaderNames.CONTENT_TYPE   -> MimeTypes.JSON,
      HeaderNames.ACCEPT         -> MimeTypes.JSON,
      HeaderNames.DATE           -> dateTimeService.timestamp.toString,
      HeaderNames.CLIENT_ID      -> "clientId",
      HeaderNames.AUTHORIZATION  -> eisInstanceConfig.headers.authorization
    )

    httpClientV2
      .get(url"$url")(hc)
      .setHeader(headers: _*)
      .executeAndDeserialise[GetEisRecordsResponse]
  }
}
