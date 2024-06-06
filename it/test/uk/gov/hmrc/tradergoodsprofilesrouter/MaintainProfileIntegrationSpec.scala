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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{FORBIDDEN, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.MaintainProfileResponse

import java.time.Instant

class MaintainProfileIntegrationSpec extends BaseIntegrationWithConnectorSpec with BeforeAndAfterEach {

  val eori          = "GB123456789001"
  val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  val dateTime      = "2021-12-17T09:30:47.456Z"
  val timestamp     = "Fri, 17 Dec 2021 09:30:47 Z"

  override def connectorPath: String = "/tgp/maintainprofile/v1"

  override def connectorName: String = "eis"

  override def beforeEach: Unit = {
    super.beforeEach()
    when(uuidService.uuid).thenReturn("d677693e-9981-4ee3-8574-654981ebe606")
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }

  "when trying to maintain a profile" - {
    "it should return a 200 ok when the request is successful" in {
      stubForEis(OK, maintainProfileEisRequest, Some(maintainProfileResponse.toString()))

      val response = wsClient
        .url(fullUrl(s"/traders/$eori"))
        .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
        .put(maintainProfileRequest)
        .futureValue

      response.status shouldBe OK
      response.json   shouldBe toJson(maintainProfileResponse.as[MaintainProfileResponse])

      verifyThatDownstreamApiWasCalled()
    }

    "it should return a 500 internal server error if EIS is unavailable" in {
      stubForEis(
        INTERNAL_SERVER_ERROR,
        maintainProfileEisRequest,
        Some(eisErrorResponse())
      )

      val response = wsClient
        .url(fullUrl(s"/traders/$eori"))
        .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
        .put(maintainProfileRequest)
        .futureValue

      response.status shouldBe INTERNAL_SERVER_ERROR
      response.json   shouldBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Internal Server Error"
      )

      verifyThatDownstreamApiWasCalled()
    }

    "it should return a 403 forbidden if the request is valid but EIS reject the request" in {
      stubForEis(FORBIDDEN, maintainProfileEisRequest)

      val response = wsClient
        .url(fullUrl(s"/traders/$eori"))
        .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
        .put(maintainProfileRequest)
        .futureValue

      response.status shouldBe FORBIDDEN
      response.json   shouldBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "FORBIDDEN",
        "message"       -> "Forbidden"
      )

      verifyThatDownstreamApiWasCalled()
    }
  }

  private def stubForEis(httpStatus: Int, requestBody: String, responseBody: Option[String] = None) = stubFor(
    put(urlEqualTo(s"$connectorPath"))
      .withRequestBody(equalToJson(requestBody))
      .withHeader("Content-Type", equalTo("application/json"))
      .withHeader("X-Forwarded-Host", equalTo("MDTP"))
      .withHeader("X-Correlation-ID", equalTo(correlationId))
      .withHeader("Date", equalTo(timestamp))
      .withHeader("Accept", equalTo("application/json"))
      .withHeader("Authorization", equalTo("Bearer dummyMaintainProfileBearerToken"))
      .withHeader("X-Client-ID", equalTo("tss"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(httpStatus)
          .withBody(responseBody.orNull)
      )
  )

  lazy val maintainProfileRequest: String =
    """
      |{
      |"actorId":"GB098765432112",
      |"ukimsNumber":"XIUKIM47699357400020231115081800",
      |"nirmsNumber":"RMS-GB-123456",
      |"niphlNumber": "6S123456"
      |}
      |""".stripMargin

  lazy val maintainProfileEisRequest: String =
    """
        |{
        |"eori" : "GB123456789001",
        |"actorId":"GB098765432112",
        |"ukimsNumber":"XIUKIM47699357400020231115081800",
        |"nirmsNumber":"RMS-GB-123456",
        |"niphlNumber": "6S123456"
        |}
        |""".stripMargin

  lazy val maintainProfileResponse: JsValue =
    Json.parse("""
        |{
        |"eori": "GB123456789012",
        |"actorId":"GB098765432112",
        |"ukimsNumber":"XIUKIM47699357400020231115081800",
        |"nirmsNumber":"RMS-GB-123456",
        |"niphlNumber": "6S123456"
        |}
        |""".stripMargin)

  private def eisErrorResponse(): String =
    Json
      .parse(
        s"""
           |{
           |  "errorDetail": {
           |    "timestamp": "2023-09-14T11:29:18Z",
           |    "correlationId": "$correlationId",
           |    "errorCode": "500",
           |    "errorMessage": "Internal Server Error",
           |    "source": "BACKEND",
           |    "sourceFaultDetail": {
           |      "detail": null
           |    }
           |  }
           |}
           |""".stripMargin
      )
      .toString()
}
