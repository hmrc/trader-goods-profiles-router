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
import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.HttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.MaintainProfileEisRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.MaintainProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService

import scala.concurrent.{ExecutionContext, Future}

class MaintainProfileConnector @Inject() (
  override val appConfig: AppConfig,
  httpClientV2: HttpClientV2,
  override val dateTimeService: DateTimeService,
  override val actorSystem: ActorSystem,
  override val configuration: Config
)(implicit val ec: ExecutionContext)
    extends BaseConnector
    with EisHttpErrorHandler {

  def maintainProfile(request: MaintainProfileEisRequest, correlationId: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[EisHttpErrorResponse, MaintainProfileResponse]] = {
    val url = appConfig.hawkConfig.maintainProfileUrl

    retryFor[MaintainProfileResponse]("maintain profile")(retryCondition) {
      httpClientV2
        .put(url"$url")
        .setHeader(
          buildHeadersWithDrop1Toggle(
            correlationId,
            appConfig.hawkConfig.maintainProfileBearerToken,
            appConfig.hawkConfig.forwardedHost
          ): _*
        )
        .withBody(Json.toJson(request))
        .execute(HttpReader[MaintainProfileResponse](correlationId, handleErrorResponse), ec)
    }
  }
}
