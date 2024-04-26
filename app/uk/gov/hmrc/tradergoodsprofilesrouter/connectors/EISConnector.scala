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
import play.api.http.Status.OK

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import java.time.{Instant}
import java.util.{UUID}

trait EISConnector {
  def fetchRecord(
    eori: String,
    recordId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HttpResponse]
}

class EISConnectorImpl(httpClientV2: HttpClientV2) extends EISConnector {

  private val baseUrl = "http://localhost:10903/tgp/getrecord/v1"

  // private val HTTP_DATE_FORMATTER =
//DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH).withZone(ZoneOffset.UTC)

  // private def nowFormatted(): String =
  //   s"${HTTP_DATE_FORMATTER.format(OffsetDateTime.now(clock.withZone(ZoneOffset.UTC)))} UTC"

  override def fetchRecord(
    eori: String,
    recordId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HttpResponse] = {
    val url           = s"$baseUrl/$eori/$recordId"
    val correlationId = UUID.randomUUID().toString
    val headers       = Seq(
      HeaderNames.CORRELATION_ID -> correlationId,
      HeaderNames.FORWARDED_HOST -> "localhost",
      HeaderNames.CONTENT_TYPE   -> MimeTypes.JSON,
      HeaderNames.ACCEPT         -> MimeTypes.JSON,
      HeaderNames.DATE           -> Instant.now().toString,
      HeaderNames.CLIENT_ID      -> "clientId",
      HeaderNames.AUTHORIZATION  -> "bearerToken"
    )

    httpClientV2
      .get(url"$url")(hc)
      .setHeader(headers: _*)
      .execute[HttpResponse]
    /*.flatMap { httpResponse =>
        httpResponse.status match {
          case OK =>
            Future.successful(
              httpResponse
            )
          case _  =>
            Future.successful(
              httpResponse
            )
        }
      }*/

  }
}
