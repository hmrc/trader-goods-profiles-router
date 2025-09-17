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
import play.api.http.Status.{BAD_GATEWAY, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.models.Retries
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse

trait BaseConnector extends Retries {

  def appConfig: AppConfig
  def dateTimeService: DateTimeService

  def retryCondition: PartialFunction[EisHttpErrorResponse, Boolean] = {
    case EisHttpErrorResponse(BAD_GATEWAY, _)                                               => true
    case EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(_, "BAD_GATEWAY", _, _)) => true
    case EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(_, "502", _, _))         => true
  }

  def commonHeaders(
    correlationId: String,
    accessToken: String,
    forwardedHost: String
  ): Seq[(String, String)] =
    Seq(
      HeaderNames.CorrelationId -> correlationId,
      HeaderNames.ForwardedHost -> forwardedHost,
      HeaderNames.Date          -> dateTimeService.timestamp.asStringHttp,
      HeaderNames.Authorization -> accessToken
    )

  def buildHeaders(
    correlationId: String,
    accessToken: String,
    forwardedHost: String
  )(implicit hc: HeaderCarrier): Seq[(String, String)] =
    commonHeaders(correlationId, accessToken, forwardedHost) ++ Seq(
      HeaderNames.Accept      -> MimeTypes.JSON,
      HeaderNames.ContentType -> MimeTypes.JSON,
      HeaderNames.ClientId    -> getClientId
    )

  def buildHeadersWithDrop1Toggle(
    correlationId: String,
    accessToken: String,
    forwardedHost: String
  )(implicit hc: HeaderCarrier): Seq[(String, String)] =
    // TODO: remove with withToggleClientID after sendClientId feature flag is safely disabled
    commonHeaders(correlationId, accessToken, forwardedHost) ++ Seq(
      HeaderNames.Accept      -> MimeTypes.JSON,
      HeaderNames.ContentType -> MimeTypes.JSON
    ).withToggleClientID

  def buildHeadersForGetMethod(
    correlationId: String,
    accessToken: String,
    forwardedHost: String
  )(implicit hc: HeaderCarrier): Seq[(String, String)] =
    // TODO: remove with withToggleClientID after sendClientId feature flag is safely disabled
    commonHeaders(correlationId, accessToken, forwardedHost) ++ Seq(
      HeaderNames.Accept -> MimeTypes.JSON
    ).withToggleClientID

  def buildHeadersForAdvice(
    correlationId: String,
    bearerToken: String,
    forwardedHost: String
  )(implicit hc: HeaderCarrier): Seq[(String, String)] =
    buildHeaders(correlationId, bearerToken, forwardedHost).filterNot(_._1 == HeaderNames.ClientId)

  def getClientId(implicit hc: HeaderCarrier): String =
    hc.headers(Seq(HeaderNames.ClientId))
      .headOption
      .getOrElse(throw new RuntimeException("Client ID is null"))
      ._2

  implicit class FilterHeaders(val value: Seq[(String, String)]) {

    def withToggleClientID(implicit hc: HeaderCarrier): Seq[(String, String)] =
      if (appConfig.sendClientId) value :+ (HeaderNames.ClientId -> getClientId)
      else value

  }
}
