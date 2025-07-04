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
import org.mockito.Mockito.*
import play.api.http.MimeTypes
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.HttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.MaintainProfileEisRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordEisResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.MaintainProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.BaseConnectorSpec

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class MaintainProfileConnectorSpec extends BaseConnectorSpec {

  private val correlationId: String = "3e8dae97-b586-4cef-8511-68ac12da9028"
  private val timestamp             = Instant.parse("2024-05-12T12:15:15.456321Z")

  private val connector = new MaintainProfileConnector(appConfig, httpClientV2, dateTimeService, as, config)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder)

    setUpAppConfig()
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.put(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(appConfig.sendClientId).thenReturn(true)
  }

  "maintain Profile" should {
    "return a 200 ok if EIS successfully maintain a profile and correct URL is used" in {

      when(requestBuilder.execute[Either[EisHttpErrorResponse, MaintainProfileResponse]](any, any))
        .thenReturn(Future.successful(Right(maintainProfileResponse)))

      val result =
        await(connector.maintainProfile(maintainProfileEisRequest.as[MaintainProfileEisRequest], correlationId))

      val expectedUrl = s"http://localhost:1234/tgp/maintainprofile/v1"
      verify(httpClientV2).put(url"$expectedUrl")
      verify(requestBuilder).setHeader(expectedHeader(correlationId, "dummyMaintainProfileBearerToken"): _*)
      verify(requestBuilder).withBody(eqTo(Json.toJson(maintainProfileEisRequest)))(any, any, any)
      verify(requestBuilder).execute(any, any)
      verify(requestBuilder)
        .execute(any[HttpReader[Either[Result, CreateOrUpdateRecordEisResponse]]], any[ExecutionContext])

      result.value mustBe maintainProfileResponse
    }

    "return a 200 ok if EIS successfully without x-client-id when sendClientid is false" in {
      when(appConfig.sendClientId).thenReturn(false)
      when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)

      when(requestBuilder.execute[Either[EisHttpErrorResponse, MaintainProfileResponse]](any, any))
        .thenReturn(Future.successful(Right(maintainProfileResponse)))

      val result =
        await(connector.maintainProfile(maintainProfileEisRequest.as[MaintainProfileEisRequest], correlationId))

      val expectedUrl = s"http://localhost:1234/tgp/maintainprofile/v1"
      verify(httpClientV2).put(url"$expectedUrl")

      verify(requestBuilder).setHeader(
        "X-Correlation-ID" -> correlationId,
        "X-Forwarded-Host" -> "MDTP",
        "Date"             -> "Sun, 12 May 2024 12:15:15 GMT",
        "Authorization"    -> "Bearer dummyMaintainProfileBearerToken",
        "Accept"           -> MimeTypes.JSON,
        "Content-Type"     -> MimeTypes.JSON
      )

      verify(requestBuilder, never()).setHeader("X-Client-ID" -> "TSS")
      verify(requestBuilder).withBody(eqTo(Json.toJson(maintainProfileEisRequest)))(any, any, any)
      verify(requestBuilder).execute(any, any)
      verify(requestBuilder)
        .execute(any[HttpReader[Either[Result, CreateOrUpdateRecordEisResponse]]], any[ExecutionContext])

      result.value mustBe maintainProfileResponse
    }

    "return an error if EIS returns one" in {
      when(requestBuilder.execute[Either[EisHttpErrorResponse, MaintainProfileResponse]](any, any))
        .thenReturn(Future.successful(Left(badRequestEISError)))

      val result =
        await(connector.maintainProfile(maintainProfileEisRequest.as[MaintainProfileEisRequest], correlationId))

      result.left.value mustBe badRequestEISError
    }
  }

  val maintainProfileEisRequest: JsValue =
    Json.parse("""
        |{
        |"eori": "GB123456789012",
        |"actorId":"GB098765432112",
        |"ukimsNumber":"XIUKIM47699357400020231115081800",
        |"nirmsNumber":"RMS-GB-123456",
        |"niphlNumber": "6S123456"
        |}
        |""".stripMargin)

  val maintainProfileResponse: MaintainProfileResponse =
    Json
      .parse("""
          |{
          |"eori": "GB123456789012",
          |"actorId":"GB098765432112",
          |"ukimsNumber":"XIUKIM47699357400020231115081800",
          |"nirmsNumber":"RMS-GB-123456",
          |"niphlNumber": "6S123456"
          |}
          |""".stripMargin)
      .as[MaintainProfileResponse]
}
