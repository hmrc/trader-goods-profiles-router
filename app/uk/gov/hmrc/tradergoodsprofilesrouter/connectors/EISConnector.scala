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
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.json.Json
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import java.net.URL
import java.time.format.DateTimeFormatter
import java.time.{Clock, OffsetDateTime, ZoneOffset}
import java.util.Locale

trait EISConnector {
  def fetchRecord(
    eori: String,
    recordId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue]
}

class EISConnectorImpl(httpClientV2: HttpClientV2, clock: Clock) extends EISConnector {

  private val baseUrl = "/tgp/getrecord/v1"

  private val HTTP_DATE_FORMATTER =
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH).withZone(ZoneOffset.UTC)

  private def nowFormatted(): String =
    s"${HTTP_DATE_FORMATTER.format(OffsetDateTime.now(clock.withZone(ZoneOffset.UTC)))} UTC"

  override def fetchRecord(
    eori: String,
    recordId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue] = {
    val url           = s"$baseUrl/$eori/$recordId"
    val correlationId = "3e8dae97-b586-4cef-8511-68ac12da9028"
    val headers       = Seq(
      HeaderNames.CORRELATION_ID -> correlationId,
      HeaderNames.FORWARDED_HOST -> "localhost",
      HeaderNames.CONTENT_TYPE   -> MimeTypes.JSON,
      HeaderNames.ACCEPT         -> MimeTypes.JSON,
      HeaderNames.DATE           -> nowFormatted,
      HeaderNames.CLIENT_ID      -> "clientId",
      HeaderNames.AUTHORIZATION  -> "bearerToken"
    )

    httpClientV2
      .get(new URL(url))(hc)
      .setHeader(headers: _*)
      .execute[HttpResponse]
      .flatMap { httpResponse =>
        httpResponse.status match {
          case OK =>
            Future.successful(
              httpResponse.json
            )
          case _  =>
            Future.successful(
              Json.obj(
                "status"   -> "error",
                "message"  -> "Failed to fetch data from EIS due to error response.",
                "eori"     -> eori,
                "recordId" -> recordId
              )
            )
        }
      }
  }
}
