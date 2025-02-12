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

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{verify, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.{Assertion, BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.MimeTypes
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.{AppConfig, HawkInstanceConfig, PegaInstanceConfig}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.{HttpReader, StatusHttpReader}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames.ClientId

import scala.concurrent.ExecutionContext

trait BaseConnectorSpec extends PlaySpec with BeforeAndAfterEach with EitherValues with EisErrorSupport {

  implicit val ec: ExecutionContext = ExecutionContext.global

  /*
   * TODO: After the Client ID is no longer needed, this should be changed and use the request without the X-Client-ID header
   * This can be changes to implicit val hc: HeaderCarrier    = HeaderCarrier()
   * as client Id has been removed form EIS header (TGP-1889,...)
   */
  implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = Seq((ClientId, "TSS")))

  val appConfig: AppConfig             = mock[AppConfig]
  val httpClientV2: HttpClientV2       = mock[HttpClientV2]
  val requestBuilder: RequestBuilder   = mock[RequestBuilder]
  val dateTimeService: DateTimeService = mock[DateTimeService]
  val config: Config               = mock[Config]
  val as: ActorSystem = ActorSystem()

  def expectedCommonHeader(
    correlationId: String,
    accessToken: String,
    forwardedHost: String = "MDTP"
  ): Seq[(String, String)] =
    Seq(
      "X-Correlation-ID" -> correlationId,
      "X-Forwarded-Host" -> forwardedHost,
      "Date"             -> "Sun, 12 May 2024 12:15:15 GMT",
      "Authorization"    -> s"Bearer $accessToken"
    )

  def expectedHeader(
    correlationId: String,
    accessToken: String,
    forwardedHost: String = "MDTP"
  ): Seq[(String, String)] =
    expectedCommonHeader(correlationId, accessToken, forwardedHost) ++ Seq(
      "Accept"       -> MimeTypes.JSON,
      "Content-Type" -> MimeTypes.JSON,
      "X-Client-ID"  -> "TSS"
    )

  def expectedHeaderWithAcceptAndContentTypeHeader(
    correlationId: String,
    accessToken: String,
    forwardedHost: String = "MDTP"
  ): Seq[(String, String)] =
    expectedCommonHeader(correlationId, accessToken, forwardedHost) ++ Seq(
      "Accept"       -> MimeTypes.JSON,
      "Content-Type" -> MimeTypes.JSON
    )

  def expectedHeaderForGetMethod(
    correlationId: String,
    accessToken: String,
    forwardedHost: String = "MDTP"
  ): Seq[(String, String)] =
    expectedCommonHeader(correlationId, accessToken, forwardedHost) ++ Seq(
      "Accept"      -> MimeTypes.JSON,
      "X-Client-ID" -> "TSS"
    )
  // TODO: After Release 2 this should be removed or become the default header builder
  def expectedHeaderForGetMethodWithoutClientId(
    correlationId: String,
    accessToken: String,
    forwardedHost: String = "MDTP"
  ): Seq[(String, String)] =
    expectedCommonHeader(correlationId, accessToken, forwardedHost) ++ Seq(
      "Accept" -> MimeTypes.JSON
    )

  def setUpAppConfig(): Unit = {
    val hawkConfig = new HawkInstanceConfig(
      protocol = "http",
      host = "localhost",
      port = 1234,
      getRecords = "/tgp/getrecords/v1",
      createRecord = "/tgp/createrecord/v1",
      removeRecord = "/tgp/removerecord/v1",
      updateRecord = "/tgp/updaterecord/v1",
      putUpdateRecord = "/tgp/puttgprecord/v1",
      maintainProfile = "/tgp/maintainprofile/v1",
      createProfile = "/tgp/createprofile/v1",
      getProfile = "/tgp/getprofile/v1",
      forwardedHost = "MDTP",
      updateRecordToken = "dummyRecordUpdateBearerToken",
      putRecordToken = "dummyPutRecordBearerToken",
      recordGetToken = "dummyRecordGetBearerToken",
      recordCreateToken = "dummyRecordCreateBearerToken",
      recordRemoveToken = "dummyRecordRemoveBearerToken",
      maintainProfileToken = "dummyMaintainProfileBearerToken",
      createProfileToken = "dummyCreateProfileBearerToken",
      getProfileToken = "dummyGetProfileBearerToken",
      getRecordsMaxSize = 500,
      getRecordsDefaultSize = 500
    )

    val pegaConfig = new PegaInstanceConfig(
      protocol = "http",
      host = "localhost",
      port = 1234,
      requestAdvice = "/tgp/createaccreditation/v1",
      forwardedHost = "MDTP",
      requestAdviceToken = "dummyAccreditationCreateBearerToken",
      getRecords = "/tgp/getrecords/v1",
      recordGetToken = "dummyRecordGetBearerToken",
      withdrawAdvise = "/tgp/withdrawaccreditation/v1",
      withdrawAdviseToken = "dummyWithdrawAdviceBearerToken",
      downloadTraderData = "/tgp/record/v1",
      downloadTraderDataToken = "dummyDownloadTraderDataToken"
    )

    when(appConfig.hawkConfig).thenReturn(hawkConfig)
    when(appConfig.pegaConfig).thenReturn(pegaConfig)
  }

  protected def verifyExecuteForHttpReader(expectedCorrelationId: String): Assertion = {
    val captor = ArgCaptor[HttpReader[Either[Result, Any]]]
    verify(requestBuilder).execute(captor.capture, any)

    val httpReader = captor.value
    httpReader.correlationId mustBe expectedCorrelationId
  }

  protected def verifyExecuteForStatusHttpReader(expectedCorrelationId: String): Assertion = {
    val captor = ArgCaptor[StatusHttpReader]
    verify(requestBuilder).execute(captor.capture, any)

    val httpReader = captor.value
    httpReader.correlationId mustBe expectedCorrelationId
  }

}
