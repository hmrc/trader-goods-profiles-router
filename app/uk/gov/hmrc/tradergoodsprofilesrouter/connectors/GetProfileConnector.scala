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
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.HttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.ProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.Error.{invalidRequestParameterError, unexpectedError}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.GetProfileSpecificError._
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetProfileConnector @Inject()(
  override val appConfig: AppConfig,
  httpClientV2: HttpClientV2,
  override val dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext)
  extends BaseConnector
    with EisHttpErrorHandler {

  def get(
    eori: String,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, ProfileResponse]] = {

    val url = s"${appConfig.hawkConfig.getProfileUrl}/$eori"

    httpClientV2.get(url"$url")
      .setHeader(headers(correlationId): _*)
      .execute(HttpReader[ProfileResponse](correlationId, handleErrorResponse), ec)
  }

  private def headers(correlationId: String): Seq[(String, String)] = {
    Seq(
      HeaderNames.CorrelationId -> correlationId,
      HeaderNames.ForwardedHost -> appConfig.hawkConfig.forwardedHost,
      HeaderNames.Accept        -> MimeTypes.JSON,
      HeaderNames.Date          -> dateTimeService.timestamp.asStringHttp,
      HeaderNames.Authorization -> appConfig.hawkConfig.getProfileBearerToken,
    )
  }

  override def parseFaultDetail(rawDetail: String, correlationId: String): Option[errors.Error] = {
    val regex = """error:\s*(\w+),\s*message:\s*(.*)""".r
    regex
      .findFirstMatchIn(rawDetail)
      .map(_ group 1)
      .collect {
        case GetProfileUnreadableJsonCode =>
          invalidRequestParameterError(GetProfileUnreadableJsonMessage, 0)
        case GetProfileInvalidCorrelationIdCode =>
          invalidRequestParameterError(InvalidOrMissingCorrelationID, 1)
        case GetProfileInvalidDateCode   =>
          invalidRequestParameterError(InvalidOrMissingRequestDate, 2)
        case GetProfileInvalidContentTypeCode        =>
          invalidRequestParameterError(InvalidOrMissingContentType, 3)
        case GetProfileInvalidAcceptCode           =>
          invalidRequestParameterError(InvalidOrMissingAccept, 4)
        case GetProfileInvalidForwardedHostCode             =>
          invalidRequestParameterError(InvalidOrMissingForwardedHost, 5)
        case InvalidOrMissingEoriCode =>
          invalidRequestParameterError(InvalidOrMissingEori, 6)
        case EoriDoesNotExistsCode =>
          invalidRequestParameterError(EoriDoesNotExists, 7)
        case other                                                    =>
          logger.warn(s"[GetProfileConnector] - Error code $other is not supported")
          unexpectedError("Unrecognised error number", other.toInt)

      }
  }

}
