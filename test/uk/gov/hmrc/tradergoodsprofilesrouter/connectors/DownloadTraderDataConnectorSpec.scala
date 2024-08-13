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
import org.mockito.MockitoSugar.{reset, when}
import play.api.http.Status.ACCEPTED
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.BaseConnectorSpec

import java.time.Instant
import scala.concurrent.Future

class DownloadTraderDataConnectorSpec extends BaseConnectorSpec {

  private val eori                  = "GB123456789011"
  private val timestamp             = Instant.parse("2024-05-12T12:15:15.456321Z")
  private val correlationId: String = "3e8dae97-b586-4cef-8511-68ac12da9028"

  private val connector = new DownloadTraderDataConnector(appConfig, httpClientV2, dateTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder)

    setUpAppConfig()
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.get(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any, any, any, any)).thenReturn(requestBuilder)
  }

  "download trader data" should {
    "return ACCEPTED when the request is accepted by EIS" in {
      when(requestBuilder.execute[Either[Result, Int]](any, any))
        .thenReturn(Future.successful(Right(ACCEPTED)))

      val result = await(connector.requestDownload(eori, correlationId))

      result.value mustBe ACCEPTED
    }

    "return any error that EIS return" in {
      val errorResponse = BadRequest("error")

      when(requestBuilder.execute[Either[Result, Int]](any, any))
        .thenReturn(Future.successful(Left(errorResponse)))

      val result = await(connector.requestDownload(eori, correlationId))

      result.left.value mustBe errorResponse
    }
  }

}
