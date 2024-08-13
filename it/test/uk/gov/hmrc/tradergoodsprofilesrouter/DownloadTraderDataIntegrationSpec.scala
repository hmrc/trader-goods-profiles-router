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

package uk.gov.hmrc.tradergoodsprofilesrouter

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, get, stubFor, urlEqualTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{ACCEPTED, FORBIDDEN}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{AuthTestSupport, PegaIntegrationSpec}

import java.time.Instant

class DownloadTraderDataIntegrationSpec extends PegaIntegrationSpec with AuthTestSupport with BeforeAndAfterEach {

  private val eori          = "GB123456789001"
  private val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  private val dateTime      = "2021-12-17T09:30:47.456Z"
  private val timestamp     = "Fri, 17 Dec 2021 09:30:47 GMT"
  private val url           = fullUrl(s"/customs/traders/goods-profiles/$eori/download")

  override def pegaConnectorPath: String = s"/tgp/downloadtraderdata/v1"

  override def beforeEach(): Unit = {
    reset(authConnector)
    withAuthorizedTrader()
    super.beforeEach()
    when(uuidService.uuid).thenReturn(correlationId)
    when(dateTimeService.timestamp).thenReturn(Instant.parse(dateTime))
  }

  "sending a request to download trader should" - {
    "return accepted when the request is accepted" in {
      stubForEis(ACCEPTED)

      val response = await(
        wsClient
          .url(url)
          .get()
      )

      response.status shouldBe ACCEPTED
    }

    "return an error from EIS if they return one" in {
      stubForEis(FORBIDDEN)

      val response = await(
        wsClient
          .url(url)
          .get()
      )

      response.status shouldBe FORBIDDEN
      response.json   shouldBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "FORBIDDEN",
        "message"       -> "Forbidden"
      )
    }
  }

  private def stubForEis(httpStatus: Int) =
    stubFor(
      get(urlEqualTo(s"$pegaConnectorPath/$eori/download"))
        .withHeader("X-Forwarded-Host", equalTo("MDTP"))
        .withHeader("X-Correlation-ID", equalTo(correlationId))
        .withHeader("Date", equalTo(timestamp))
        .withHeader("Authorization", equalTo("Bearer dummyDownloadTraderDataToken"))
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
        )
    )
}
