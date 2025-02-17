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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.ProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.BaseConnectorSpec

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

class GetProfileConnectorSpec extends BaseConnectorSpec {

  private val eori          = "123"
  private val timestamp     = Instant.parse("2024-05-12T12:15:15.456321Z")
  private val correlationId = UUID.randomUUID().toString
  private val sut           = new GetProfileConnector(appConfig, httpClientV2, dateTimeService, as, config)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder)

    setUpAppConfig()
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.get(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any, any, any, any, any)).thenReturn(requestBuilder)

  }
  "get" should {
    "return 200" in {
      when(requestBuilder.execute[Either[EisHttpErrorResponse, ProfileResponse]](any, any))
        .thenReturn(Future.successful(Right(ProfileResponse(eori, "123", None, None, None))))

      val result = await(sut.get(eori, correlationId))

      result.value mustBe ProfileResponse(eori, "123", None, None, None)

      withClue("should send a request with the right parameter") {
        val expectedUrl = s"http://localhost:1234/tgp/getprofile/v1/$eori"
        verify(httpClientV2).get(eqTo(url"$expectedUrl"))(any)
        verify(requestBuilder).setHeader(
          expectedHeaderForGetMethodWithoutClientId(correlationId, "dummyGetProfileBearerToken"): _*
        )
        verifyExecuteForHttpReader(correlationId)
      }
    }

    "return an error if EIS return an error" in {
      when(requestBuilder.execute[Either[EisHttpErrorResponse, ProfileResponse]](any, any))
        .thenReturn(Future.successful(Left(badRequestEISError)))

      val result = await(sut.get(eori, correlationId))

      result.left.value mustBe badRequestEISError
    }
  }

}
