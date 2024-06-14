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
import play.api.http.MimeTypes
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
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

  private val connector = new GetRecordsConnector(appConfig, httpClientV2, dateTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder)

    setUpAppConfig()
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.get(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any, any, any, any, any, any, any)).thenReturn(requestBuilder)
  }

  "fetchRecord" should {
    "fetch a record successfully" in {
      val response: GetEisRecordsResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]

      when(requestBuilder.execute[Either[Result, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      val result = await(connector.fetchRecord(eori, recordId, correlationId))

      result.value mustBe response
    }

    "return an error whenEIS return an error" in {
      when(requestBuilder.execute[Either[Result, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Left(BadRequest("error"))))

      val result = await(connector.fetchRecord(eori, recordId, correlationId))

      result.left.value mustBe BadRequest("error")
    }

    "send a request with the right parameters" in {
      val response: GetEisRecordsResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]

      when(requestBuilder.execute[Any](any, any))
        .thenReturn(Future.successful(Right(response)))

      await(connector.fetchRecord(eori, recordId, correlationId))

      val expectedUrl = s"http://localhost:1234/tgp/getrecords/v1/$eori/$recordId"
      verify(httpClientV2).get(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(expectedHeader: _*)
      verifyExecuteWithParams(correlationId)
    }
  }

  "fetchRecords" should {
    "fetch multiple records successfully" in {
      val response: GetEisRecordsResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]

      when(requestBuilder.execute[Either[Result, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      val result = await(connector.fetchRecords(eori, correlationId))

      result.value mustBe response
    }

    "return an error if EIS return an error" in {
      when(requestBuilder.execute[Either[Result, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Left(BadRequest("error"))))

      val result = await(connector.fetchRecord(eori, recordId, correlationId))

      result.left.value mustBe BadRequest("error")
    }

    "send a request with the right url for fetch records" in {
      val response: GetEisRecordsResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]

      when(requestBuilder.execute[Either[Result, GetEisRecordsResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      await(connector.fetchRecords(eori, correlationId, Some(timestamp), Some(1), Some(1)))

      val expectedLastUpdateDate = Instant.parse("2024-05-12T12:15:15Z")
      val expectedUrl            =
        s"http://localhost:1234/tgp/getrecords/v1/$eori?lastUpdatedDate=$expectedLastUpdateDate&page=1&size=1"
      verify(httpClientV2).get(url"$expectedUrl")
      verify(requestBuilder).setHeader(expectedHeader: _*)
      verifyExecuteWithParams(correlationId)
    }
  }

  def expectedHeader: Seq[(String, String)] =
    Seq(
      "X-Correlation-ID" -> correlationId,
      "X-Forwarded-Host" -> "MDTP",
      "Content-Type"     -> MimeTypes.JSON,
      "Accept"           -> MimeTypes.JSON,
      "Date"             -> "Sun, 12 May 2024 12:15:15 GMT",
      "X-Client-ID"      -> "TSS",
      "Authorization"    -> "Bearer dummyRecordGetBearerToken"
    )
}