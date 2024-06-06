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

package uk.gov.hmrc.tradergoodsprofilesrouter.support

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{verify, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.MimeTypes
import play.api.mvc.Result
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.{AppConfig, EISInstanceConfig}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.HttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse

trait BaseConnectorSpec extends PlaySpec with BeforeAndAfterEach with EitherValues with GetRecordsDataSupport {

  val appConfig: AppConfig           = mock[AppConfig]
  val httpClientV2: HttpClientV2     = mock[HttpClientV2]
  val requestBuilder: RequestBuilder = mock[RequestBuilder]

  def buildHeaders(correlationId: String, accessToken: String) = Seq(
    "X-Correlation-ID" -> correlationId,
    "X-Forwarded-Host" -> "MDTP",
    "Content-Type"     -> MimeTypes.JSON,
    "Accept"           -> MimeTypes.JSON,
    "Date"             -> "Sun, 12 May 2024 12:15:15 Z",
    "X-Client-ID"      -> "TSS",
    "Authorization"    -> s"Bearer $accessToken"
  )

  def setUpAppConfig() =
    when(appConfig.eisConfig).thenReturn(
      new EISInstanceConfig(
        "http",
        "localhost",
        1234,
        "/tgp/getrecords/v1",
        "/tgp/createrecord/v1",
        "/tgp/removerecord/v1",
        "/tgp/updaterecord/v1",
        "/tgp/createaccreditation/v1",
        "MDTP",
        "dummyRecordUpdateBearerToken",
        "dummyRecordGetBearerToken",
        "dummyRecordCreateBearerToken",
        "dummyRecordRemoveBearerToken",
        "dummyAccreditationCreateBearerToken",
        "dummyMaintainProfileBearerToken"
      )
    )

  def verifyExecuteWithParams(expectedCorrelationId: String) = {
    val captor = ArgCaptor[HttpReads[Either[Result, GetEisRecordsResponse]]]
    verify(requestBuilder).execute(captor.capture, any)

    val httpReader = captor.value
    httpReader.asInstanceOf[HttpReader[Either[Result, Any]]].correlationId mustBe expectedCorrelationId
  }

}
