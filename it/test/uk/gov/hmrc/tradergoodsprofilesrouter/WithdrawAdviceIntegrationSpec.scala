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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, delete, stubFor, urlEqualTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.AuthTestSupport

import java.time.Instant
import java.util.UUID

class WithdrawAdviceIntegrationSpec
  extends BaseIntegrationWithConnectorSpec
    with AuthTestSupport
    with BeforeAndAfterEach {


  override def connectorPath: String = "/tgp/Withdrawaccreditation/v1"
  private val url           = fullUrl(s"/traders/$eori/records/$recordId/advice?withdrawReason=didnotlikeit")
  private val eori = "GB123456789001"
  private val recordId = UUID.randomUUID().toString
  private val correlationId = UUID.randomUUID().toString


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(authConnector)
    withAuthorizedTrader()

    when(uuidService.uuid).thenReturn(correlationId)
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }
  "withdraw advice" - {
    "withdraw advice successfully" in {
      stubForEisRequest

      val result = await(wsClient.url(url)
        .withHttpHeaders("X-Client-ID" -> "tss")
        .delete())

      result.status shouldBe NO_CONTENT
    }

    "return an error" - {
      "eis return BAD_REQUEST" in {
        stubForEisBadRequest

        val result = await(wsClient.url(url)
          .withHttpHeaders("X-Client-ID" -> "tss")
          .delete())

        result.status shouldBe BAD_REQUEST
        result.json shouldBe Json.obj(
          "correlationId" -> correlationId,
          "code" -> "BAD_REQUEST",
          "message" -> "Bad Request",
          "errors" -> Json.arr(
            Json.obj(
              "code" -> "INVALID_REQUEST_PARAMETER",
              "message" -> "X-Correlation-ID was missing from Header or is in the wrong format",
              "errorNumber" -> 1001
            ),
            Json.obj(
              "code" -> "INVALID_REQUEST_PARAMETER",
              "message" -> "X-Forwarded-Host was missing from Header os is in the wrong format",
              "errorNumber" -> 1002
            )
          )
        )
      }
    }
  }

  private def stubForEisRequest = {
    stubFor(delete(urlEqualTo(s"$connectorPath"))
      .willReturn(
        aResponse()
          .withStatus(NO_CONTENT)
      ))
  }

  private def stubForEisBadRequest = {
    stubFor(delete(urlEqualTo(s"$connectorPath"))
      .willReturn(
        aResponse()
          .withStatus(BAD_REQUEST)
          .withBody(badRequestErrorResponse.toString())
      ))
  }

  def badRequestErrorResponse =
    Json.parse(
      s"""
        |{
        | "errorDetail": {
        |   "timestamp": "2024-03-18T16:42:28Z",
        |   "correlationId": "7ba38231-1848-407e-a242-4ff748068ddf",
        |   "errorCode": "400",
        |   "errorMessage": "Bad Request",
        |   "source": "BACKEND",
        |   "sourceFaultDetail": {
        |   "detail": [
        |     "error: E001, message: Header parameter XCorrelationID is mandatory.",
        |     "error: E002, message: Header parameter XForwardedHost is mandatory."
        |     ]
        |   }
        | }
        |}
        |""".stripMargin)
}
