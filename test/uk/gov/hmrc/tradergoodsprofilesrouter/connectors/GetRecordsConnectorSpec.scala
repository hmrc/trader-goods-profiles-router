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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verify, when}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{BaseConnectorSpec, GetRecordsDataSupport}

import java.time.Instant
import scala.concurrent.Future

class GetRecordsConnectorSpec extends BaseConnectorSpec with GetRecordsDataSupport {

  private val eori                  = "GB123456789011"
  private val recordId              = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  private val timestamp             = Instant.parse("2024-05-12T12:15:15.456321Z")
  private val correlationId: String = "3e8dae97-b586-4cef-8511-68ac12da9028"

  private val connector = new GetRecordsConnector(appConfig, httpClientV2, dateTimeService, as, config)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder)

    setUpAppConfig()
    when(appConfig.sendClientId).thenReturn(true)
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.get(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)

  }

  "fetchRecord" should {
    val response: GetEisRecordsResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]
    "fetch a record successfully" in {

      when(requestBuilder.execute[Either[EisHttpErrorResponse, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      val result = await(connector.fetchRecord(eori, recordId, correlationId, appConfig.hawkConfig.getRecordsUrl))

      result.value mustBe response
    }

    "handle errors" when {
      "EIS return an error" in {
        when(requestBuilder.execute[Either[EisHttpErrorResponse, GetEisRecordsResponse]](any, any))
          .thenReturn(Future.successful(Left(badRequestEISError)))

        val result = await(
          connector.fetchRecord(eori, recordId, correlationId, s"http://localhost:1234/tgp/getrecords/v1")
        )

        result.left.value mustBe badRequestEISError
      }
    }

    "send a request with the right parameters" when {
      "sendClientId feature flag is false" in {
        when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
        val response: GetEisRecordsResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]
        when(appConfig.sendClientId).thenReturn(false)

        when(requestBuilder.execute[Any](any, any))
          .thenReturn(Future.successful(Right(response)))

        await(connector.fetchRecord(eori, recordId, correlationId, "http://localhost:1234/tgp/getrecords/v1"))

        val expectedUrl = s"http://localhost:1234/tgp/getrecords/v1/$eori/$recordId"
        verify(httpClientV2).get(eqTo(url"$expectedUrl"))(any)
        verify(requestBuilder).setHeader(
          expectedHeaderForGetMethodWithoutClientId(correlationId, "dummyRecordGetBearerToken"): _*
        )
        verifyExecuteForHttpReader(correlationId)
      }

      // TODO: After Release 2 remove x-client-id from headers
      "sendClientId feature flag is true" in {
        when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[Any](any, any))
          .thenReturn(Future.successful(Right(getEisRecordsResponseData.as[GetEisRecordsResponse])))

        await(connector.fetchRecord(eori, recordId, correlationId, "http://localhost:1234/tgp/getrecords/v1"))

        val expectedUrl = s"http://localhost:1234/tgp/getrecords/v1/$eori/$recordId"
        verify(httpClientV2).get(eqTo(url"$expectedUrl"))(any)

        val expectedHeaderWithClientId =
          expectedHeaderForGetMethod(correlationId, "dummyRecordGetBearerToken")
        verify(requestBuilder).setHeader(expectedHeaderWithClientId: _*)
        verifyExecuteForHttpReader(correlationId)
      }
    }

  }

  "fetchRecords" should {
    "fetch multiple records successfully" in {
      val defaultSize                     = 500
      val response: GetEisRecordsResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]

      when(requestBuilder.execute[Either[EisHttpErrorResponse, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      val result = await(connector.fetchRecords(eori, correlationId, defaultSize))

      result.value mustBe response
    }

    "return an error if EIS return an error" in {
      when(requestBuilder.execute[Either[EisHttpErrorResponse, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Left(badRequestEISError)))

      val result = await(connector.fetchRecord(eori, recordId, correlationId, appConfig.hawkConfig.getRecordsUrl))

      result.left.value mustBe badRequestEISError
    }

    "return an error if EIS return an error with static stub" in {
      when(requestBuilder.execute[Either[EisHttpErrorResponse, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Left(badRequestEISError)))

      val result = await(connector.fetchRecord(eori, recordId, correlationId, appConfig.hawkConfig.getRecordsUrl))

      result.left.value mustBe badRequestEISError
    }

    "send a request with the right url for fetch records" in {
      val response: GetEisRecordsResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]

      when(requestBuilder.execute[Either[EisHttpErrorResponse, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      await(connector.fetchRecords(eori, correlationId, 1, Some(1), Some(timestamp)))

      val expectedLastUpdateDate = Instant.parse("2024-05-12T12:15:15Z")
      val expectedUrl            =
        s"http://localhost:1234/tgp/getrecords/v1/$eori?lastUpdatedDate=$expectedLastUpdateDate&page=1&size=1"
      verify(httpClientV2).get(url"$expectedUrl")
      verify(requestBuilder).setHeader(expectedHeaderForGetMethod(correlationId, "dummyRecordGetBearerToken"): _*)
      verifyExecuteForHttpReader(correlationId)
    }
  }
}
