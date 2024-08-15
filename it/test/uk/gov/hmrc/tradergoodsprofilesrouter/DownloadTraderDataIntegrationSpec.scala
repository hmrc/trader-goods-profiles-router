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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{AuthTestSupport, PegaIntegrationSpec}

import java.time.Instant

class DownloadTraderDataIntegrationSpec extends PegaIntegrationSpec with AuthTestSupport with BeforeAndAfterEach {

  private val eori          = "GB123456789001"
  private val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  private val dateTime      = "2021-12-17T09:30:47.456Z"
  private val url           = fullUrl(s"/customs/traders/goods-profiles/$eori/download")

  override def pegaConnectorPath: String = s"/tgp/record/v1"

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

    "return a Internal Server error" in {
      stubForEisWithResponseBody(INTERNAL_SERVER_ERROR, internalServerErrorResponse)

      val result = await(
        wsClient
          .url(url)
          .get()
      )

      result.status shouldBe INTERNAL_SERVER_ERROR
      result.json   shouldBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Internal Server Error"
      )
    }

    "should handle BAD_REQUEST errors thrown by EIS" in {
      stubForEisWithResponseBody(BAD_REQUEST, badRequestErrorResponse)

      val result = await(
        wsClient
          .url(url)
          .get()
      )

      result.status shouldBe BAD_REQUEST
      result.json   shouldBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "BAD_REQUEST",
        "message"       -> "Bad Request",
        "errors"        -> Json.arr(
          Json.obj(
            "code"        -> "INVALID_REQUEST_PARAMETER",
            "message"     -> "X-Correlation-ID was missing from Header or is in the wrong format",
            "errorNumber" -> 1
          ),
          Json.obj(
            "code"        -> "INVALID_REQUEST_PARAMETER",
            "message"     -> "Request Date was missing from Header or is in the wrong format",
            "errorNumber" -> 2
          ),
          Json.obj(
            "code"        -> "INVALID_REQUEST_PARAMETER",
            "message"     -> "X-Forwarded-Host was missing from Header or is in the wrong format",
            "errorNumber" -> 5
          ),
          Json.obj(
            "code"        -> "INVALID_REQUEST_PARAMETER",
            "message"     -> "The EORI number has been provided in the wrong format",
            "errorNumber" -> 6
          ),
          Json.obj(
            "code"        -> "INVALID_REQUEST_PARAMETER",
            "message"     -> "EORI number does not have a TGP",
            "errorNumber" -> 7
          ),
          Json.obj(
            "code"        -> "INVALID_REQUEST_PARAMETER",
            "message"     -> "EORI number is not linked to any records in the database",
            "errorNumber" -> 37
          )
        )
      )
    }
  }

  private def stubForEis(httpStatus: Int) =
    stubFor(
      get(urlEqualTo(s"$pegaConnectorPath/$eori/download"))
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
        )
    )

  private def stubForEisWithResponseBody(httpStatus: Int, response: JsValue) =
    stubFor(
      get(urlEqualTo(s"$pegaConnectorPath/$eori/download"))
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
            .withBody(response.toString())
        )
    )

  private def badRequestErrorResponse =
    Json.parse(s"""
                  |{
                  | "errorDetail": {
                  |   "timestamp": "2024-03-18T16:42:28Z",
                  |   "correlationId": "7ba38231-1848-407e-a242-4ff748068ddf",
                  |   "errorCode": "400",
                  |   "errorMessage": "Bad Request",
                  |   "source": "BACKEND",
                  |   "sourceFaultDetail": {
                  |   "detail": [
                  |     "error: 001, message: Invalid Header",
                  |     "error: 002, message: Invalid Header",
                  |     "error: 005, message: Invalid Header",
                  |     "error: 006, message: Invalid Request Parameter",
                  |     "error: 007, message: Invalid Request Parameter",
                  |     "error: 037, message: Invalid Request Parameter"
                  |     ]
                  |   }
                  | }
                  |}
                  |""".stripMargin)

  private def internalServerErrorResponse: JsValue =
    Json.parse(s"""
                  |{
                  | "errorDetail": {
                  |   "timestamp": "2024-03-18T16:42:28Z",
                  |   "correlationId": "7ba38231-1848-407e-a242-4ff748068ddf",
                  |   "errorCode": "500",
                  |   "errorMessage": "Bad Request",
                  |   "source": "BACKEND",
                  |   "sourceFaultDetail": {
                  |   "detail": null
                  |   }
                  | }
                  |}
                  |""".stripMargin)
}
