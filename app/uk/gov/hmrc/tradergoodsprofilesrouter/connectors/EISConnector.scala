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
import play.api.http.Status.OK
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.json.Json

trait EISConnector {
  def fetchRecords(
      eori: String,
      lastUpdatedDate: Option[String],
      page: Option[Int],
      size: Option[Int],
      hc: HeaderCarrier
  )(implicit ec: ExecutionContext): Future[JsValue]
}

class EISConnectorImpl(httpClientV2: HttpClientV2) extends EISConnector {

  private val baseUrl = "https://stub.eis.service"

  override def fetchRecords(
      eori: String,
      lastUpdatedDate: Option[String],
      page: Option[Int],
      size: Option[Int],
      hc: HeaderCarrier
  )(implicit ec: ExecutionContext): Future[JsValue] = {
    val url = s"$baseUrl/$eori"

    httpClientV2.get(url"url")(hc).execute[HttpResponse].flatMap {
      httpResponse =>
        httpResponse.status match {
          case OK =>
            Future.successful(
              httpResponse.json
            )
          case _ =>
            Future.successful(
              Json.obj(
                "status" -> "error",
                "message" -> "Failed to fetch data from EIS due to error response.",
                "eori" -> eori,
                "lastUpdatedDate" -> lastUpdatedDate,
                "page" -> page,
                "size" -> size
              )
            )
        }
    }
  }
}
