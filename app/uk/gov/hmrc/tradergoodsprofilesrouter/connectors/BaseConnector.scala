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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

trait BaseConnector {

  def appConfig: AppConfig
  def dateTimeService: DateTimeService

  protected def buildHeaders(correlationId: String, accessToken: String, forwardedHost: String)(implicit
    hc: HeaderCarrier
  ): Seq[(String, String)] =
    Seq(
      HeaderNames.CorrelationId -> correlationId,
      HeaderNames.ForwardedHost -> forwardedHost,
      HeaderNames.Accept        -> MimeTypes.JSON,
      HeaderNames.Date          -> dateTimeService.timestamp.asStringHttp,
      HeaderNames.ClientId      -> getClientId,
      HeaderNames.Authorization -> accessToken,
      HeaderNames.ContentType   -> MimeTypes.JSON
    )

  protected def buildHeadersForCreateMethod(correlationId: String, accessToken: String, forwardedHost: String)(implicit
                                                                                                               hc: HeaderCarrier
  ): Seq[(String, String)] = {
    val headers = Seq(
      HeaderNames.CorrelationId -> correlationId,
      HeaderNames.ForwardedHost -> forwardedHost,
      HeaderNames.Accept        -> MimeTypes.JSON,
      HeaderNames.Date          -> dateTimeService.timestamp.asStringHttp,
      HeaderNames.Authorization -> accessToken,
      HeaderNames.ContentType   -> MimeTypes.JSON
    )

    //ToDo: remove this and return a header without the client ID after drop1.1.
    // For drop1.1 client Id has been removed (TGP-1889)
    // TODO: After Drop 1.1 this should be removed - Ticket: TGP-2014
    if (appConfig.isDrop1_1_enabled) headers
    else headers :+ (HeaderNames.ClientId -> getClientId)
  }


  // TODO: Remove this method- TGP-2014
  protected def buildHeadersForMaintainProfile(
    correlationId: String,
    accessToken: String,
    forwardedHost: String
  )(implicit hc: HeaderCarrier): Seq[(String, String)] = {

    val headers = Seq(
      HeaderNames.CorrelationId -> correlationId,
      HeaderNames.ForwardedHost -> forwardedHost,
      HeaderNames.Accept        -> MimeTypes.JSON,
      HeaderNames.Date          -> dateTimeService.timestamp.asStringHttp,
      HeaderNames.Authorization -> accessToken,
      HeaderNames.ContentType   -> MimeTypes.JSON
    )

    //TODO: Remove the client-id after the drop1.1 - Ticket-2014
    if (appConfig.isDrop1_1_enabled) headers
    else headers :+ (HeaderNames.ClientId -> getClientId)
  }

  protected def buildHeadersForGetMethod(
    correlationId: String,
    accessToken: String,
    forwardedHost: String
  )(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    val headers = Seq(
      HeaderNames.CorrelationId -> correlationId,
      HeaderNames.ForwardedHost -> forwardedHost,
      HeaderNames.Accept        -> MimeTypes.JSON,
      HeaderNames.Date          -> dateTimeService.timestamp.asStringHttp,
      HeaderNames.Authorization -> accessToken
    )

    //ToDo: remove this and return a header without the client ID after drop1.1.
    // For drop1.1 client Id has been removed (TGP-1889)
    // TODO: After Drop 1.1 this should be removed - Ticket: TGP-2014
    if (appConfig.isDrop1_1_enabled) headers
    else headers :+ (HeaderNames.ClientId -> getClientId)
  }

  protected def buildHeadersForUpdateMethod(correlationId: String, accessToken: String, forwardedHost: String)(implicit
    hc: HeaderCarrier
  ): Seq[(String, String)] = {
    val headers = Seq(
      HeaderNames.CorrelationId -> correlationId,
      HeaderNames.ForwardedHost -> forwardedHost,
      HeaderNames.Accept        -> MimeTypes.JSON,
      HeaderNames.Date          -> dateTimeService.timestamp.asStringHttp,
      HeaderNames.Authorization -> accessToken,
      HeaderNames.ContentType   -> MimeTypes.JSON
    )
    // TODO For drop1.1 client Id has been removed (TGP-1903)
    if (appConfig.isDrop1_1_enabled) headers
    else headers :+ (HeaderNames.ClientId -> getClientId)
  }

  protected def buildHeadersForAdvice(correlationId: String, bearerToken: String, forwardedHost: String)(implicit
    hc: HeaderCarrier
  ): Seq[(String, String)] =
    buildHeaders(correlationId, bearerToken, forwardedHost).filterNot(_._1 == HeaderNames.ClientId)

  def getClientId(implicit hc: HeaderCarrier): String =
    hc.headers(Seq(HeaderNames.ClientId))
      .headOption
      .getOrElse(throw new RuntimeException("Client ID is null"))
      ._2
}
