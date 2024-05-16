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
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import sttp.model.Uri.UriContext
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse

import java.time.Instant

class GetMultipleRecordsIntegrationSpec extends BaseIntegrationWithConnectorSpec with BeforeAndAfterEach {

  val eori                           = "GB123456789001"
  val correlationId                  = "d677693e-9981-4ee3-8574-654981ebe606"
  val dateTime                       = "2021-12-17T09:30:47.456Z"
  val timestamp                      = "Fri, 17 Dec 2021 09:30:47 Z"
  override def connectorPath: String = s"/tgp/getrecords/v1"
  override def connectorName: String = "eis"

  override def beforeEach: Unit = {
    super.beforeEach()
    when(uuidService.uuid).thenReturn(correlationId)
    when(dateTimeService.timestamp).thenReturn(Instant.parse(dateTime))
  }

  "attempting to get records, when" - {
    "the request is" - {
      "valid without optional query parameter" in {
        stubForEis(OK, Some(getMultipleRecordResponseData.toString()))

        val response = wsClient
          .url(fullUrl(s"/$eori"))
          .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
          .get()
          .futureValue

        response.status shouldBe OK
        response.json   shouldBe Json.toJson(getMultipleRecordResponseData.as[GetEisRecordsResponse])

        verifyThatDownstreamApiWasCalled()
      }
      "valid with optional query parameter lastUpdatedDate, page and size" in {
        stubForEis(OK, Some(getMultipleRecordResponseData.toString()), Some(dateTime), Some(1), Some(1))

        val response = wsClient
          .url(fullUrl(s"/$eori?lastUpdatedDate=$dateTime&page=1&size=1"))
          .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
          .get()
          .futureValue

        response.status shouldBe OK
        response.json   shouldBe Json.toJson(getMultipleRecordResponseData.as[GetEisRecordsResponse])

        verifyThatDownstreamApiWasCalled()
      }
      "valid with optional query parameter page and size" in {
        stubForEis(OK, Some(getMultipleRecordResponseData.toString()), None, Some(1), Some(1))

        val response = wsClient
          .url(fullUrl(s"/$eori?page=1&size=1"))
          .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
          .get()
          .futureValue

        response.status shouldBe OK
        response.json   shouldBe Json.toJson(getMultipleRecordResponseData.as[GetEisRecordsResponse])

        verifyThatDownstreamApiWasCalled()
      }
      "valid with optional query parameter page" in {
        stubForEis(OK, Some(getMultipleRecordResponseData.toString()), None, Some(1), None)

        val response = wsClient
          .url(fullUrl(s"/$eori?page=1"))
          .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
          .get()
          .futureValue

        response.status shouldBe OK
        response.json   shouldBe Json.toJson(getMultipleRecordResponseData.as[GetEisRecordsResponse])

        verifyThatDownstreamApiWasCalled()
      }
      "valid with optional query parameter lastUpdatedDate" in {
        stubForEis(OK, Some(getMultipleRecordResponseData.toString()), Some(dateTime))

        val response = wsClient
          .url(fullUrl(s"/$eori?lastUpdatedDate=$dateTime"))
          .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
          .get()
          .futureValue

        response.status shouldBe OK
        response.json   shouldBe Json.toJson(getMultipleRecordResponseData.as[GetEisRecordsResponse])

        verifyThatDownstreamApiWasCalled()
      }
      "valid but the integration call fails with response:" - {
        "Forbidden" in {
          stubForEis(FORBIDDEN)

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe FORBIDDEN
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "FORBIDDEN",
            "message"       -> "Forbidden"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Not Found" in {
          stubForEis(NOT_FOUND)

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe NOT_FOUND
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "NOT_FOUND",
            "message"       -> "Not Found"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Method Not Allowed" in {
          stubForEis(METHOD_NOT_ALLOWED)

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe METHOD_NOT_ALLOWED
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "METHOD_NOT_ALLOWED",
            "message"       -> "Method Not Allowed"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Service Unavailable" in {
          stubForEis(SERVICE_UNAVAILABLE)

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe SERVICE_UNAVAILABLE
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "SERVICE_UNAVAILABLE",
            "message"       -> "Service Unavailable"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("500", "Internal Server Error")))

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> "d677693e-9981-4ee3-8574-654981ebe606",
            "code"          -> "INTERNAL_SERVER_ERROR",
            "message"       -> "Internal Server Error"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 200 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("200", "Internal Server Error")))

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "INVALID_OR_EMPTY_PAYLOAD",
            "message"       -> "Invalid Response Payload or Empty payload"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 400 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("400", "Internal Error Response")))

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "INTERNAL_ERROR_RESPONSE",
            "message"       -> "Internal Error Response"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 401 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("401", "Unauthorised")))

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNAUTHORIZED",
            "message"       -> "Unauthorized"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 404 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("404", "Not Found")))

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "NOT_FOUND",
            "message"       -> "Not Found"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 405 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("405", "Method Not Allowed")))

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "METHOD_NOT_ALLOWED",
            "message"       -> "Method Not Allowed"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 502 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("502", "Bad Gateway")))

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_GATEWAY",
            "message"       -> "Bad Gateway"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 503 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("503", "Service Unavailable")))

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "SERVICE_UNAVAILABLE",
            "message"       -> "Service Unavailable"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with unknown errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("501", "Not Implemented")))

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNKNOWN",
            "message"       -> "Unknown Error"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with unexpected error" in {
          val eisErrorResponseBody = s"""
                                        | {
                                        |    "invalid": "error"
                                        |  }
                                        |""".stripMargin

          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponseBody))

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNEXPECTED_ERROR",
            "message"       -> "Unexpected Error"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request for invalid or missing EORI" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/null"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(BAD_REQUEST)
                  .withBody(s"""
                               |{
                               |  "errorDetail": {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "400",
                               |    "errorMessage": "Invalid request parameter",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": [
                               |      "error: 006, message: Missing or invalid mandatory request parameter EORI"
                               |      ]
                               |    }
                               |  }
                               |}
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/null"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"    -> "006",
                "message" -> "Mandatory field eori was missing from body"
              )
            )
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request for EORI does not exists in database" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(BAD_REQUEST)
                  .withBody(s"""
                               |{
                               |  "errorDetail": {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "400",
                               |    "errorMessage": "Invalid request parameter",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": [
                               |      "error: 007, message: EORI does not exist in the database"
                               |      ]
                               |    }
                               |  }
                               |}
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"    -> "007",
                "message" -> "EORI number does not have a TGP"
              )
            )
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request for invalid query parameter lastUpdatedDate" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(BAD_REQUEST)
                  .withBody(s"""
                               |{
                               |  "errorDetail": {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "400",
                               |    "errorMessage": "Invalid request parameter",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": [
                               |      "error: 028, message: Invalid optional request parameter lastUpdatedDate"
                               |      ]
                               |    }
                               |  }
                               |}
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"    -> "028",
                "message" -> "The URL parameter lastUpdatedDate is in the wrong format"
              )
            )
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request for invalid query parameter page" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(BAD_REQUEST)
                  .withBody(s"""
                               |{
                               |  "errorDetail": {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "400",
                               |    "errorMessage": "Invalid request parameter",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": [
                               |      "error: 029, message: Invalid optional request parameter page"
                               |      ]
                               |    }
                               |  }
                               |}
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"    -> "029",
                "message" -> "The URL parameter page is in the wrong format"
              )
            )
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request for invalid query parameter size" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(BAD_REQUEST)
                  .withBody(s"""
                               |{
                               |  "errorDetail": {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "400",
                               |    "errorMessage": "Invalid request parameter",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": [
                               |      "error: 030, message: Invalid optional request parameter size"
                               |      ]
                               |    }
                               |  }
                               |}
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"    -> "030",
                "message" -> "The URL parameter size is in the wrong format"
              )
            )
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request with unexpected error" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(BAD_REQUEST)
                  .withBody(s"""
                               |{
                               |  "errorDetail": {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "400",
                               |    "errorMessage": "Invalid request parameter",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": [
                               |      "error: 040, message: undefined"
                               |      ]
                               |    }
                               |  }
                               |}
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"    -> "UNEXPECTED_ERROR",
                "message" -> "Unexpected Error"
              )
            )
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request with unable to parse the detail" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(BAD_REQUEST)
                  .withBody(s"""
                               |{
                               |  "errorDetail": {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "400",
                               |    "errorMessage": "Invalid request parameter",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": ["error"]
                               |    }
                               |  }
                               |}
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "statusCode" -> 500,
            "message"    -> s"Unable to parse fault detail for correlation Id: $correlationId"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request with invalid json" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(BAD_REQUEST)
                  .withBody(s"""
                               | {
                               |    "invalid": "error"
                               |  }
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNEXPECTED_ERROR",
            "message"       -> "Unexpected Error"
          )

          verifyThatDownstreamApiWasCalled()
        }
      }
      "invalid with missing mandatory header" in {

        val response = wsClient
          .url(fullUrl(s"/$eori"))
          .withHttpHeaders(("Content-Type", "application/json"))
          .get()
          .futureValue

        response.status shouldBe BAD_REQUEST
        response.json   shouldBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "BAD_REQUEST",
          "message"       -> "Missing mandatory header X-Client-Id"
        )

        verifyThatDownstreamApiWasNotCalled()
      }
    }
  }

  private def stubForEis(
    httpStatus: Int,
    body: Option[String] = None,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  ) = {

    val uri =
      uri"$connectorPath/$eori?lastUpdatedDate=$lastUpdatedDate&page=$page&size=$size"
    stubFor(
      get(urlEqualTo(s"$uri"))
        .withHeader("Content-Type", equalTo("application/json"))
        .withHeader("X-Forwarded-Host", equalTo("MDTP"))
        .withHeader("X-Correlation-ID", equalTo(correlationId))
        .withHeader("Date", equalTo(timestamp))
        .withHeader("Accept", equalTo("application/json"))
        .withHeader("Authorization", equalTo("bearerToken"))
        .withHeader("X-Client-Id", equalTo("tss"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(httpStatus)
            .withBody(body.orNull)
        )
    )
  }

  private def eisErrorResponse(errorCode: String, errorMessage: String): String =
    Json
      .parse(
        s"""
       |{
       |  "errorDetail": {
       |    "timestamp": "2023-09-14T11:29:18Z",
       |    "correlationId": "$correlationId",
       |    "errorCode": "$errorCode",
       |    "errorMessage": "$errorMessage",
       |    "source": "BACKEND",
       |    "sourceFaultDetail": {
       |      "detail": null
       |    }
       |  }
       |}
       |""".stripMargin
      )
      .toString()

  def getMultipleRecordResponseData: JsValue = Json.parse(s"""
                                                                           |{
                                                                           |"goodsItemRecords":
                                                                           |[
                                                                           |  {
                                                                           |    "eori": "$eori",
                                                                           |    "actorId": "GB1234567890",
                                                                           |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
                                                                           |    "traderRef": "BAN001001",
                                                                           |    "comcode": "104101000",
                                                                           |    "accreditationStatus": "Not requested",
                                                                           |    "goodsDescription": "Organic bananas",
                                                                           |    "countryOfOrigin": "EC",
                                                                           |    "category": 3,
                                                                           |    "assessments": [
                                                                           |      {
                                                                           |        "assessmentId": "abc123",
                                                                           |        "primaryCategory": 1,
                                                                           |        "condition": {
                                                                           |          "type": "abc123",
                                                                           |          "conditionId": "Y923",
                                                                           |          "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
                                                                           |          "conditionTraderText": "Excluded product"
                                                                           |        }
                                                                           |      }
                                                                           |    ],
                                                                           |    "supplementaryUnit": 500,
                                                                           |    "measurementUnit": "square meters(m^2)",
                                                                           |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
                                                                           |    "comcodeEffectiveToDate": "",
                                                                           |    "version": 1,
                                                                           |    "active": true,
                                                                           |    "toReview": false,
                                                                           |    "reviewReason": null,
                                                                           |    "declarable": "IMMI declarable",
                                                                           |    "ukimsNumber": "XIUKIM47699357400020231115081800",
                                                                           |    "nirmsNumber": "RMS-GB-123456",
                                                                           |    "niphlNumber": "6 S12345",
                                                                           |    "locked": false,
                                                                           |    "srcSystemName": "CDAP",
                                                                           |    "createdDateTime": "2024-11-18T23:20:19Z",
                                                                           |    "updatedDateTime": "2024-11-18T23:20:19Z"
                                                                           |  },
                                                                           |    {
                                                                           |    "eori": "$eori",
                                                                           |    "actorId": "GB1234567890",
                                                                           |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
                                                                           |    "traderRef": "BAN001001",
                                                                           |    "comcode": "104101000",
                                                                           |    "accreditationStatus": "Not requested",
                                                                           |    "goodsDescription": "Organic bananas",
                                                                           |    "countryOfOrigin": "EC",
                                                                           |    "category": 3,
                                                                           |    "assessments": [
                                                                           |      {
                                                                           |        "assessmentId": "abc123",
                                                                           |        "primaryCategory": 1,
                                                                           |        "condition": {
                                                                           |          "type": "abc123",
                                                                           |          "conditionId": "Y923",
                                                                           |          "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
                                                                           |          "conditionTraderText": "Excluded product"
                                                                           |        }
                                                                           |      }
                                                                           |    ],
                                                                           |    "supplementaryUnit": 500,
                                                                           |    "measurementUnit": "square meters(m^2)",
                                                                           |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
                                                                           |    "comcodeEffectiveToDate": "",
                                                                           |    "version": 1,
                                                                           |    "active": true,
                                                                           |    "toReview": false,
                                                                           |    "reviewReason": null,
                                                                           |    "declarable": "IMMI declarable",
                                                                           |    "ukimsNumber": "XIUKIM47699357400020231115081800",
                                                                           |    "nirmsNumber": "RMS-GB-123456",
                                                                           |    "niphlNumber": "6 S12345",
                                                                           |    "locked": false,
                                                                           |    "srcSystemName": "CDAP",
                                                                           |    "createdDateTime": "2024-11-18T23:20:19Z",
                                                                           |    "updatedDateTime": "2024-11-18T23:20:19Z"
                                                                           |  }
                                                                           |],
                                                                           |"pagination":
                                                                           | {
                                                                           |   "totalRecords": 2,
                                                                           |   "currentPage": 0,
                                                                           |   "totalPages": 1,
                                                                           |   "nextPage": null,
                                                                           |   "prevPage": null
                                                                           | }
                                                                           |}
                                                                           |""".stripMargin)
}