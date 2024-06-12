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
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.{HttpReader, OtherHttpReader}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.accreditationrequests.{RequestEisAccreditationRequest, TraderDetails}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.MaintainProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EISConnector @Inject() (
  override val appConfig: AppConfig,
  httpClientV2: HttpClientV2,
  override val dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext)
    extends BaseConnector
    with EisHttpErrorHandler {

  def requestAccreditation(request: TraderDetails, correlationId: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[Result, Int]] = {
    val url = appConfig.eisConfig.createAccreditationUrl

    val accreditationEisRequest = RequestEisAccreditationRequest(request, dateTimeService.timestamp.asStringHttp)
    httpClientV2
      .post(url"$url")
      .setHeader(buildHeadersForAccreditation(correlationId, appConfig.eisConfig.createAccreditationBearerToken): _*)
      .withBody(Json.toJson(accreditationEisRequest))
      .execute(OtherHttpReader[Int](correlationId, handleErrorResponse), ec)
  }

}
