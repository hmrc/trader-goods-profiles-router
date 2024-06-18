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
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.LegacyHttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.MaintainProfileEisRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.MaintainProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService

import scala.concurrent.{ExecutionContext, Future}

class MaintainProfileConnector @Inject() (
  override val appConfig: AppConfig,
  httpClientV2: HttpClientV2,
  override val dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext)
    extends BaseConnector
    with LegacyEisHttpErrorHandler {

  def maintainProfile(request: MaintainProfileEisRequest, correlationId: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[Result, MaintainProfileResponse]] = {
    val url = appConfig.eisConfig.maintainProfileUrl
    httpClientV2
      .put(url"$url")
      .setHeader(buildHeaders(correlationId, appConfig.eisConfig.maintainProfileBearerToken): _*)
      .withBody(Json.toJson(request))
      .execute(LegacyHttpReader[MaintainProfileResponse](correlationId, legacyHandleErrorResponse), ec)
  }
}
