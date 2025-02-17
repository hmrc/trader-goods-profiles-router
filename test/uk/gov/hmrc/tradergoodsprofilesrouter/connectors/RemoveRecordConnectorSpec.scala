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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, verify, when}
import play.api.http.MimeTypes
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.BodyWritable
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.BaseConnectorSpec

import java.net.URL
import java.time.Instant
import scala.concurrent.Future

class RemoveRecordConnectorSpec extends BaseConnectorSpec {

  private val eori                  = "GB123456789011"
  private val actorId               = "GB123456789011"
  private val recordId              = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  private val timestamp             = Instant.parse("2024-05-12T12:15:15.456321Z")
  private val correlationId: String = "3e8dae97-b586-4cef-8511-68ac12da9028"

  private val connector = new RemoveRecordConnector(appConfig, httpClientV2, dateTimeService, as, config)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, httpClientV2, dateTimeService, requestBuilder)

    setUpAppConfig()
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(httpClientV2.put(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(appConfig.sendClientId).thenReturn(true)
    when(appConfig.sendAcceptHeader).thenReturn(true)
  }

  "remove a record successfully" in {
    when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any, any))
      .thenReturn(Future.successful(Right(200)))

    val result = await(connector.removeRecord(eori, recordId, actorId, correlationId))

    result.value mustBe 200
  }

  "send a request with the right url for remove record when sendClientId feature flag is true" in {
    when(appConfig.sendClientId).thenReturn(true)

    when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any(), any()))
      .thenReturn(Future.successful(Right(200)))

    val result = await(connector.removeRecord(eori, recordId, actorId, correlationId))

    val expectedUrl = new URL("http://localhost:1234/tgp/removerecord/v1")

    // ✅ Fix: Use `eq(expectedUrl)`
    verify(httpClientV2).put(url"$expectedUrl")
    verify(requestBuilder).setHeader(expectedHeader(correlationId, "dummyRecordRemoveBearerToken"): _*)

    val expectedJson = Json.obj("eori" -> eori, "recordId" -> recordId, "actorId" -> actorId)
    val jsonCaptor   = ArgumentCaptor.forClass(classOf[JsValue])

    verify(requestBuilder).withBody(jsonCaptor.capture())(any[BodyWritable[JsValue]], any(), any())
    jsonCaptor.getValue mustBe expectedJson

    verifyExecuteForStatusHttpReader(correlationId)
    result.value mustBe 200
  }

  "send a request with the right url for remove record when sendClientId feature flag is false" in {
    when(appConfig.sendClientId).thenReturn(false)
    val hc: HeaderCarrier = HeaderCarrier()

    when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any, any))
      .thenReturn(Future.successful(Right(200)))
    when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)

    val result =
      await(connector.removeRecord(eori, recordId, actorId, correlationId)(hc))

    val expectedUrl = s"http://localhost:1234/tgp/removerecord/v1"
    verify(httpClientV2).put(url"$expectedUrl")(hc)

    val headersCaptor: ArgumentCaptor[Seq[(String, String)]] = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
    verify(requestBuilder).setHeader(headersCaptor.capture(): _*)

    val capturedHeaders = headersCaptor.getValue

    capturedHeaders    must contain allOf (
      "X-Correlation-ID" -> correlationId,
      "X-Forwarded-Host" -> "MDTP",
      "Authorization"    -> "Bearer dummyRecordRemoveBearerToken"
    )

    capturedHeaders    must not contain ("X-Client-ID" -> "TSS")

    val jsonCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
    verify(requestBuilder).withBody(jsonCaptor.capture())(any(), any(), any())

    val capturedJson = jsonCaptor.getValue

    capturedJson mustBe Json.obj(
      "eori"     -> eori,
      "recordId" -> recordId,
      "actorId"  -> actorId
    )

    // Verify execute is called with the correct StatusHttpReader
    verifyExecuteForStatusHttpReader(correlationId)

    // Validate result
    result.value mustBe 200
  }

  "send a request with the right url for remove record when sendAcceptHeader feature flag is true" in {
    when(appConfig.sendAcceptHeader).thenReturn(true)

    // Stub setHeader by matching the entire Seq as varargs.
    when(requestBuilder.setHeader(any[Seq[(String, String)]]: _*))
      .thenReturn(requestBuilder)

    // Stub withBody and execute.
    when(requestBuilder.withBody(any[JsValue])(any, any, any))
      .thenReturn(requestBuilder)
    when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any, any))
      .thenReturn(Future.successful(Right(200)))

    val result = await(connector.removeRecord(eori, recordId, actorId, correlationId))

    val expectedUrl = s"http://localhost:1234/tgp/removerecord/v1"
    verify(httpClientV2).put(url"$expectedUrl")

    // Capture the headers passed to setHeader.
    val headersCaptor: ArgumentCaptor[Seq[(String, String)]] =
      ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
    verify(requestBuilder).setHeader(headersCaptor.capture(): _*)

    // The captured value is already a Scala Seq.
    val capturedHeaders: Seq[(String, String)] = headersCaptor.getValue

    capturedHeaders    must contain allOf (
      "X-Correlation-ID" -> correlationId,
      "Authorization"    -> "Bearer dummyRecordRemoveBearerToken",
      "X-Forwarded-Host" -> "MDTP",
      "Accept"           -> "application/json"
    )

    // Use the alias meq for equality matcher to avoid Scala's eq conflict.
    verify(requestBuilder).withBody(
      meq(Json.obj("eori" -> eori, "recordId" -> recordId, "actorId" -> actorId))
    )(any, any, any)

    verifyExecuteForStatusHttpReader(correlationId)

    result.value mustBe 200
  }

  "send a request with the right url for remove record when sendAcceptHeader feature flag is false" in {
    when(appConfig.sendAcceptHeader).thenReturn(false)
    when(appConfig.sendClientId).thenReturn(true)

    when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any, any))
      .thenReturn(Future.successful(Right(200)))
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)

    val result =
      await(connector.removeRecord(eori, recordId, actorId, correlationId))

    val expectedUrl = s"http://localhost:1234/tgp/removerecord/v1"
    verify(httpClientV2).put(url"$expectedUrl")
    verify(requestBuilder).setHeader(
      expectedHeaderWithoutAcceptHeader(correlationId, "dummyRecordRemoveBearerToken"): _*
    )
    verify(requestBuilder).withBody(
      meq(Json.obj("eori" -> eori, "recordId" -> recordId, "actorId" -> actorId))
    )(any[play.api.libs.ws.BodyWritable[JsValue]], any, any)

    verifyExecuteForStatusHttpReader(correlationId)

    result.value mustBe 200
  }

  "return an error if EIS return an error" in {
    when(requestBuilder.execute[Either[EisHttpErrorResponse, Int]](any, any))
      .thenReturn(Future.successful(Left(badRequestEISError)))

    val result = await(connector.removeRecord(eori, recordId, actorId, correlationId))

    result.left.value mustBe badRequestEISError
  }

  private def expectedHeaderWithoutAcceptHeader(
    correlationId: String,
    accessToken: String,
    forwardedHost: String = "MDTP"
  ): Seq[(String, String)] = Seq(
    "X-Correlation-ID" -> correlationId,
    "X-Forwarded-Host" -> forwardedHost,
    "Date"             -> "Sun, 12 May 2024 12:15:15 GMT",
    "Authorization"    -> s"Bearer $accessToken",
    "Content-Type"     -> MimeTypes.JSON,
    "X-Client-ID"      -> "TSS"
  )
}
