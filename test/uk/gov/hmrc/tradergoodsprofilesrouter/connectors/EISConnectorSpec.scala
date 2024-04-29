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
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import play.api.http.MimeTypes
import play.api.http.Status.OK
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import java.time.{Instant, OffsetDateTime}
import scala.concurrent.{ExecutionContext, Future}

class EISConnectorSpec extends PlaySpec with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()
  "fetchRecord " should {
    "return a single record" in {
      val mockHttpClientV2 = mock[HttpClientV2](RETURNS_DEEP_STUBS)
      val eori             = "GB123456789011"
      val recordId         = "12345"
      val eisConnector     = new EISConnectorImpl(mockHttpClientV2)
      val headers          = Seq(
        HeaderNames.CORRELATION_ID -> "3e8dae97-b586-4cef-8511-68ac12da9028",
        HeaderNames.FORWARDED_HOST -> "localhost",
        HeaderNames.CONTENT_TYPE   -> MimeTypes.JSON,
        HeaderNames.ACCEPT         -> MimeTypes.JSON,
        HeaderNames.DATE           -> "2023-01-01T00:00:00Z",
        HeaderNames.CLIENT_ID      -> "clientId",
        HeaderNames.AUTHORIZATION  -> "bearerToken"
      )
      val url              = "http://localhost:10903/tgp/getrecord/v1/GB123456789011/12345"

      when(
        mockHttpClientV2
          .get(url"$url")(hc)
          .setHeader(headers: _*)
          .execute
      )
        .thenReturn(Future.successful(HttpResponse(200, "Ok")))
      val response = eisConnector.fetchRecord(eori, recordId).value.value.get
      assertResult(response.status)(OK)
      assertResult(response.body)("Ok")
    }
  }

}
