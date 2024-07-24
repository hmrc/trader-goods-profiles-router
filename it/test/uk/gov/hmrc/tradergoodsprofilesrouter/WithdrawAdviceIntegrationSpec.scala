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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, put, stubFor, urlEqualTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{AuthTestSupport, PegaIntegrationSpec}

import java.time.Instant
import java.util.UUID

class WithdrawAdviceIntegrationSpec extends PegaIntegrationSpec with AuthTestSupport with BeforeAndAfterEach {

  override def pegaConnectorPath: String = "/tgp/withdrawaccreditation/v1"
  private val eori                       = "GB123456789001"
  private val recordId                   = UUID.randomUUID().toString
  private val correlationId              = UUID.randomUUID().toString
  private val url                        = fullUrl(s"/traders/$eori/records/$recordId/advice")

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(authConnector)
    withAuthorizedTrader()

    when(uuidService.uuid).thenReturn(correlationId)
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }
  "withdraw advice" - {
    "withdraw advice successfully" in {
      stubForEis(NO_CONTENT)

      val result = await(
        wsClient
          .url(url)
          .withHttpHeaders(
            ("Content-Type", "application/json"),
            ("Accept", "application/vnd.hmrc.1.0+json"),
            ("X-Client-ID", "tss")
          )
          .put(Json.obj())
      )

      result.status shouldBe NO_CONTENT
      verifyThatDownstreamApiWasCalled(pegaConnectorPath)
    }

    "return a BAD_REQUEST error" - {
      "eis return BAD_REQUEST" in {
        stubForEis(BAD_REQUEST, badRequestErrorResponse)

        val result = await(
          wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(Json.obj())
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
              "message"     -> "X-Forwarded-Host was missing from Header os is in the wrong format",
              "errorNumber" -> 5
            ),
            Json.obj(
              "code"        -> "INVALID_REQUEST_PARAMETER",
              "message"     -> "Content-Type was missing from Header or is in the wrong format",
              "errorNumber" -> 3
            ),
            Json.obj(
              "code"        -> "INVALID_REQUEST_PARAMETER",
              "message"     -> "Accept was missing from Header or is in the wrong format",
              "errorNumber" -> 4
            ),
            Json.obj(
              "code"        -> "INVALID_REQUEST_PARAMETER",
              "message"     -> "Mandatory withdrawDate was missing from body",
              "errorNumber" -> 1013
            ),
            Json.obj(
              "code"        -> "INVALID_REQUEST_PARAMETER",
              "message"     -> "Mandatory goodsItems was missing from body",
              "errorNumber" -> 1014
            ),
            Json.obj(
              "code"        -> "INVALID_REQUEST_PARAMETER",
              "message"     -> "The request has already been completed and a new request cannot be requested",
              "errorNumber" -> 1017
            )
          )
        )
      }

      "eis return BAD_REQUEST when EIS has not error message" in {
        stubForEis(BAD_REQUEST, badRequestErrorResponseWithoutError)

        val result = await(
          wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(Json.obj())
        )

        result.status shouldBe BAD_REQUEST
        result.json   shouldBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "BAD_REQUEST",
          "message"       -> "Bad Request"
        )
      }
    }

    "return a FORBIDDEN error when EIS return a 403 with no payload" in {
      stubForEis(FORBIDDEN)

      val result = await(
        wsClient
          .url(url)
          .withHttpHeaders(
            ("Content-Type", "application/json"),
            ("Accept", "application/vnd.hmrc.1.0+json"),
            ("X-Client-ID", "tss")
          )
          .put(Json.obj())
      )

      result.status shouldBe FORBIDDEN
      result.json   shouldBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "FORBIDDEN",
        "message"       -> "Forbidden"
      )
    }

    "return a NOT_FOUND error when EIS return a 404 with no payload" in {
      stubForEis(NOT_FOUND)

      val result = await(
        wsClient
          .url(url)
          .withHttpHeaders(
            ("Content-Type", "application/json"),
            ("Accept", "application/vnd.hmrc.1.0+json"),
            ("X-Client-ID", "tss")
          )
          .put(Json.obj())
      )

      result.status shouldBe NOT_FOUND
      result.json   shouldBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "NOT_FOUND",
        "message"       -> "Not Found"
      )
    }

    "return a BAD_GATEWAY error when EIS return a 502 with no payload" in {
      stubForEis(BAD_GATEWAY)

      val result = await(
        wsClient
          .url(url)
          .withHttpHeaders(
            ("Content-Type", "application/json"),
            ("Accept", "application/vnd.hmrc.1.0+json"),
            ("X-Client-ID", "tss")
          )
          .put(Json.obj())
      )

      result.status shouldBe BAD_GATEWAY
      result.json   shouldBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "BAD_GATEWAY",
        "message"       -> "Bad Gateway"
      )
    }

    "return a SERVICE_UNAVAILABLE error when EIS return a 503 with no payload" in {
      stubForEis(SERVICE_UNAVAILABLE)

      val result = await(
        wsClient
          .url(url)
          .withHttpHeaders(
            ("Content-Type", "application/json"),
            ("Accept", "application/vnd.hmrc.1.0+json"),
            ("X-Client-ID", "tss")
          )
          .put(Json.obj())
      )

      result.status shouldBe SERVICE_UNAVAILABLE
      result.json   shouldBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "SERVICE_UNAVAILABLE",
        "message"       -> "Service Unavailable"
      )
    }

    "return a METHOD_NOT_ALLOWED error when EIS return a 405 with no payload" in {
      stubForEis(METHOD_NOT_ALLOWED)

      val result = await(
        wsClient
          .url(url)
          .withHttpHeaders(
            ("Content-Type", "application/json"),
            ("Accept", "application/vnd.hmrc.1.0+json"),
            ("X-Client-ID", "tss")
          )
          .put(Json.obj())
      )

      result.status shouldBe METHOD_NOT_ALLOWED
      result.json   shouldBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "METHOD_NOT_ALLOWED",
        "message"       -> "Method Not Allowed"
      )
    }

    "return a Internal Server error" in {
      stubForEis(INTERNAL_SERVER_ERROR, internalServerErrorResponse(500))

      val result = await(
        wsClient
          .url(url)
          .withHttpHeaders(
            ("Content-Type", "application/json"),
            ("Accept", "application/vnd.hmrc.1.0+json"),
            ("X-Client-ID", "tss")
          )
          .put(Json.obj())
      )

      result.status shouldBe INTERNAL_SERVER_ERROR
      result.json   shouldBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Internal Server Error"
      )
    }

    val table = Table(
      ("errodCode", "code", "message"),
      (401, "UNAUTHORIZED", "Unauthorized"),
      (405, "METHOD_NOT_ALLOWED", "Method Not Allowed"),
      (500, "INTERNAL_SERVER_ERROR", "Internal Server Error"),
      (502, "BAD_GATEWAY", "Bad Gateway"),
      (503, "SERVICE_UNAVAILABLE", "Service Unavailable")
    )

    forAll(table) { (errorCode, code, message) =>
      s"return a Internal Server error when error code is $errorCode" in {
        stubForEis(INTERNAL_SERVER_ERROR, internalServerErrorResponse(errorCode))

        val result = await(
          wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(Json.obj())
        )

        result.status shouldBe INTERNAL_SERVER_ERROR
        result.json   shouldBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> code,
          "message"       -> message
        )
      }
    }

    "return an error if error response contains unrecognised error code" in {
      stubForEis(400, badRequestResponseWithUnrecognisedError)

      val result = await(
        wsClient
          .url(url)
          .withHttpHeaders(
            ("Content-Type", "application/json"),
            ("Accept", "application/vnd.hmrc.1.0+json"),
            ("X-Client-ID", "tss")
          )
          .put(Json.obj())
      )

      result.json shouldBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "BAD_REQUEST",
        "message"       -> "Bad Request",
        "errors"        -> Json.arr(
          Json.obj(
            "code"        -> "UNEXPECTED_ERROR",
            "message"     -> "Unrecognised error number",
            "errorNumber" -> 6
          )
        )
      )
    }
  }

  private def stubForEis(httpStatus: Int) =
    stubFor(
      put(urlEqualTo(s"$pegaConnectorPath"))
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
        )
    )

  private def stubForEis(httpStatus: Int, responseBody: JsValue) =
    stubFor(
      put(urlEqualTo(s"$pegaConnectorPath"))
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
            .withBody(responseBody.toString())
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
        |     "error: E001, message: Header parameter XCorrelationID is mandatory",
        |     "error: E002, message: Header parameter XForwardedHost is mandatory",
        |     "error: E003, message: Header parameter Content Type is mandatory",
        |     "error: E004, message: Header parameter Accept is mandatory",
        |     "error: E005, message: Withdraw Date is mandatory",
        |     "error: E006, message: At least one goods Item is mandatory",
        |     "error: E009, message: The decision is already made",
        |     "status: Fail"
        |     ]
        |   }
        | }
        |}
        |""".stripMargin)

  private def badRequestErrorResponseWithoutError =
    Json.parse(s"""
         |{
         | "errorDetail": {
         |   "timestamp": "2024-03-18T16:42:28Z",
         |   "correlationId": "7ba38231-1848-407e-a242-4ff748068ddf",
         |   "errorCode": "400",
         |   "errorMessage": "Bad Request",
         |   "source": "BACKEND",
         |   "sourceFaultDetail": {
         |   "detail": ["status: Fail"]
         |   }
         | }
         |}
         |""".stripMargin)

  private def internalServerErrorResponse(errorCode: Int) =
    Json.parse(s"""
         |{
         | "errorDetail": {
         |   "timestamp": "2024-03-18T16:42:28Z",
         |   "correlationId": "7ba38231-1848-407e-a242-4ff748068ddf",
         |   "errorCode": "$errorCode",
         |   "errorMessage": "Bad Request",
         |   "source": "BACKEND",
         |   "sourceFaultDetail": {
         |   "detail": null
         |   }
         | }
         |}
         |""".stripMargin)

  private def badRequestResponseWithUnrecognisedError = Json.parse(s"""
       |{
       | "errorDetail": {
       |   "timestamp": "2024-03-18T16:42:28Z",
       |   "correlationId": "$correlationId",
       |   "errorCode": "400",
       |   "errorMessage": "Bad Request",
       |   "source": "BACKEND",
       |   "sourceFaultDetail": {
       |   "detail": [
       |     "error: 006, message: Header parameter XCorrelationID is mandatory",
       |     "status: Fail"
       |     ]
       |   }
       | }
       |}
       |""".stripMargin)
}
