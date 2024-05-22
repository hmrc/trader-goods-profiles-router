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
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport

import java.time.Instant

class GetSingleRecordIntegrationSpec
    extends BaseIntegrationWithConnectorSpec
    with GetRecordsDataSupport
    with BeforeAndAfterEach {

  val eori                           = "GB123456789001"
  val recordId                       = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  val correlationId                  = "d677693e-9981-4ee3-8574-654981ebe606"
  val dateTime                       = "2021-12-17T09:30:47.456Z"
  val timestamp                      = "Fri, 17 Dec 2021 09:30:47 Z"
  override def connectorPath: String = s"/tgp/getrecords/v1"
  override def connectorName: String = "eis"

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(uuidService.uuid).thenReturn(correlationId)
    when(dateTimeService.timestamp).thenReturn(Instant.parse(dateTime))
  }

  "attempting to get records, when" - {
    "the request is" - {
      "valid" in {
        stubForEis(OK, Some(getEisRecordsResponseData.toString()))

        val response = wsClient
          .url(fullUrl(s"/$eori/records/$recordId"))
          .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
          .get()
          .futureValue

        response.status shouldBe OK
        response.json   shouldBe Json.toJson(getEisRecordsResponseData.as[GetEisRecordsResponse].goodsItemRecords.head)

        verifyThatDownstreamApiWasCalled()
      }
      "valid but the integration call fails with response:" - {
        "Forbidden" in {
          stubForEis(FORBIDDEN)

          val response = await(
            wsClient
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
            get(urlEqualTo(s"$connectorPath/null/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-ID", equalTo("tss"))
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
              .url(fullUrl("/null/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
              .get()
          )

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Mandatory field eori was missing from body or is in the wrong format",
                "errorNumber" -> 6
              )
            )
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request for EORI does not exists in database" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-ID", equalTo("tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
              .get()
          )

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "EORI number does not have a TGP",
                "errorNumber" -> 7
              )
            )
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request for recordId does not exists in database and Invalid/Missing recordId" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/null"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-ID", equalTo("tss"))
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
                               |      "error: 025, message: Invalid request parameter recordId",
                               |      "error: 026, message: recordId doesn’t exist in the database"
                               |      ]
                               |    }
                               |  }
                               |}
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/null"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
              .get()
          )

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "The recordId has been provided in the wrong format",
                "errorNumber" -> 25
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "The requested recordId to update doesn’t exist",
                "errorNumber" -> 26
              )
            )
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request with unexpected error" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-ID", equalTo("tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
              .get()
          )

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"        -> "UNEXPECTED_ERROR",
                "message"     -> "Unrecognised error number",
                "errorNumber" -> 40
              )
            )
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request with unable to parse the detail" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-ID", equalTo("tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
              .get()
          )

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNEXPECTED_ERROR",
            "message"       -> s"Unable to parse fault detail for correlation Id: $correlationId"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request with invalid json" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-ID", equalTo("tss"))
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
              .url(fullUrl(s"/$eori/records/$recordId"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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
          .url(fullUrl(s"/$eori/records/$recordId"))
          .withHttpHeaders(("Content-Type", "application/json"))
          .get()
          .futureValue

        response.status shouldBe BAD_REQUEST
        response.json   shouldBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "BAD_REQUEST",
          "message"       -> "Missing mandatory header X-Client-ID"
        )

        verifyThatDownstreamApiWasNotCalled()
      }
    }
  }

  private def stubForEis(httpStatus: Int, body: Option[String] = None) = stubFor(
    get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
      .withHeader("Content-Type", equalTo("application/json"))
      .withHeader("X-Forwarded-Host", equalTo("MDTP"))
      .withHeader("X-Correlation-ID", equalTo(correlationId))
      .withHeader("Date", equalTo(timestamp))
      .withHeader("Accept", equalTo("application/json"))
      .withHeader("Authorization", equalTo("bearerToken"))
      .withHeader("X-Client-ID", equalTo("tss"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(httpStatus)
          .withBody(body.orNull)
      )
  )

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
       |      "detail":null
       |    }
       |  }
       |}
       |""".stripMargin
      )
      .toString()
}
