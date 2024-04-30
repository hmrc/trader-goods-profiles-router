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

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito.{RETURNS_DEEP_STUBS, verify}
import org.mockito.MockitoSugar.when
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.MimeTypes
import play.api.http.Status.OK
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.{AppConfig, EISInstanceConfig, Headers}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class EISConnectorSpec extends PlaySpec with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()
  val appConfig                     = mock[AppConfig]

  when(appConfig.eisConfig).thenReturn(
    new EISInstanceConfig(
      "http",
      "localhost",
      1234,
      "/tgp/getrecords/v1",
      Headers("bearerToken")
    )
  )

  "fetchRecord " should {
    "return a single record" in {
      val mockHttpClientV2    = mock[HttpClientV2](RETURNS_DEEP_STUBS)
      val mockDateTimeService = mock[DateTimeService]
      val eori                = "GB123456789011"
      val recordId            = "12345"
      val dateTime            = "2023-01-01T00:00:00Z"
      val eisConnector        = new EISConnectorImpl(appConfig.eisConfig, mockHttpClientV2, mockDateTimeService)

      when(mockDateTimeService.timestamp).thenReturn(Instant.parse(dateTime))
      val headers = Seq(
        HeaderNames.CORRELATION_ID -> "3e8dae97-b586-4cef-8511-68ac12da9028",
        HeaderNames.FORWARDED_HOST -> appConfig.eisConfig.host,
        HeaderNames.CONTENT_TYPE   -> MimeTypes.JSON,
        HeaderNames.ACCEPT         -> MimeTypes.JSON,
        HeaderNames.DATE           -> dateTime,
        HeaderNames.CLIENT_ID      -> "clientId",
        HeaderNames.AUTHORIZATION  -> appConfig.eisConfig.headers.authorization
      )

      val requestHeaderCaptor: ArgumentCaptor[Seq[(String, String)]] =
        ArgumentCaptor.forClass(classOf[Seq[(String, String)]])

      when(
        mockHttpClientV2
          .get(any)(any)
          .setHeader(any)
          .execute
      )
        .thenReturn(Future.successful(HttpResponse(200, "Ok")))

      val response       = eisConnector.fetchRecord(eori, recordId).value.value.get
      verify(mockHttpClientV2.get(any)(any), Mockito.atLeast(1)).setHeader(requestHeaderCaptor.capture(): _*)
      val requestHeaders = requestHeaderCaptor.getValue
      assertResult(response.status)(OK)
      assertResult(response.body)("Ok")
      assertResult(headers.length)(requestHeaders.length)
      val map            = requestHeaders.toMap
      val uuid           = map.get(HeaderNames.CORRELATION_ID).value
      val forwardedHost  = map.get(HeaderNames.FORWARDED_HOST).value
      val contentType    = map.get(HeaderNames.CONTENT_TYPE).value
      val accept         = map.get(HeaderNames.ACCEPT).value
      val date           = map.get(HeaderNames.DATE).value
      val clientId       = map.get(HeaderNames.CLIENT_ID).value
      val authorization  = map.get(HeaderNames.AUTHORIZATION).value
      assert(uuid.nonEmpty)
      assertResult(forwardedHost)("localhost")
      assertResult(contentType)(MimeTypes.JSON)
      assertResult(accept)(MimeTypes.JSON)
      assertResult(date)(dateTime)
      assertResult(clientId)("clientId")
      assertResult(authorization)("bearerToken")
    }
  }

}
