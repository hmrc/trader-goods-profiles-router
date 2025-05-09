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
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.GetRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{AuthTestSupport, GetRecordsDataSupport, HawkIntegrationSpec}

import java.time.Instant

class GetSingleRecordIntegrationSpec
    extends HawkIntegrationSpec
    with AuthTestSupport
    with GetRecordsDataSupport
    with BeforeAndAfterEach {

  private val eori                       = "GB123456789001"
  private val recordId                   = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  private val correlationId              = "d677693e-9981-4ee3-8574-654981ebe606"
  private val dateTime                   = "2021-12-17T09:30:47.456Z"
  private val url                        = fullUrl(s"/traders/$eori/records/$recordId")
  override def hawkConnectorPath: String = s"/tgp/getrecords/v1"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset()
    withAuthorizedTrader()

    when(uuidService.uuid).thenReturn(correlationId)
    when(dateTimeService.timestamp).thenReturn(Instant.parse(dateTime))
  }

  override def extraApplicationConfig: Map[String, Any] =
    super.extraApplicationConfig ++ Map(
      "features.sendClientId" -> true
    )

  "attempting to get records, when" - {
    "the request is" - {

      "valid" in {
        stubForEis(OK, Some(getEisRecordsResponseData.toString()))

        val response = sendRequestAndWait(url)

        response.status shouldBe OK
        response.json   shouldBe Json.toJson(
          getRecordsResponseDataWithNiphlStrippedOfDashes.as[GetRecordsResponse].goodsItemRecords.head
        )

        verifyThatDownstreamApiWasCalled(hawkConnectorPath)
      }

      "valid but the integration call fails with response:" - {
        "Forbidden" in {
          stubForEis(FORBIDDEN)

          val response = sendRequestAndWait(url)

          response.status shouldBe FORBIDDEN
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "FORBIDDEN",
            "message"       -> "Forbidden"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Not Found" in {
          stubForEis(NOT_FOUND)

          val response = sendRequestAndWait(url)

          response.status shouldBe NOT_FOUND
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "NOT_FOUND",
            "message"       -> "Not Found"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Method Not Allowed" in {
          stubForEis(METHOD_NOT_ALLOWED)

          val response = sendRequestAndWait(url)

          response.status shouldBe METHOD_NOT_ALLOWED
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "METHOD_NOT_ALLOWED",
            "message"       -> "Method Not Allowed"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Service Unavailable" in {
          stubForEis(SERVICE_UNAVAILABLE)

          val response = sendRequestAndWait(url)

          response.status shouldBe SERVICE_UNAVAILABLE
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "SERVICE_UNAVAILABLE",
            "message"       -> "Service Unavailable"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("500", "Internal Server Error")))

          val response = sendRequestAndWait(url)

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> "d677693e-9981-4ee3-8574-654981ebe606",
            "code"          -> "INTERNAL_SERVER_ERROR",
            "message"       -> "Internal Server Error"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 200 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("200", "Internal Server Error")))

          val response = sendRequestAndWait(url)

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "INVALID_OR_EMPTY_PAYLOAD",
            "message"       -> "Invalid Response Payload or Empty payload"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 400 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("400", "Internal Error Response")))

          val response = sendRequestAndWait(url)

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "INTERNAL_ERROR_RESPONSE",
            "message"       -> "Internal Error Response"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 401 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("401", "Unauthorised")))

          val response = sendRequestAndWait(url)

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNAUTHORIZED",
            "message"       -> "Unauthorized"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 404 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("404", "Not Found")))

          val response = sendRequestAndWait(url)

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "NOT_FOUND",
            "message"       -> "Not Found"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 405 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("405", "Method Not Allowed")))

          val response = sendRequestAndWait(url)

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "METHOD_NOT_ALLOWED",
            "message"       -> "Method Not Allowed"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 502 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("502", "Bad Gateway")))

          val response = sendRequestAndWait(url)

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_GATEWAY",
            "message"       -> "Bad Gateway"
          )

          verifyThatDownstreamApiWasRetried(hawkConnectorPath)
        }
        "Internal Server Error with 503 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("503", "Service Unavailable")))

          val response = sendRequestAndWait(url)

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "SERVICE_UNAVAILABLE",
            "message"       -> "Service Unavailable"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with unknown errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("501", "Not Implemented")))

          val response = sendRequestAndWait(url)

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNKNOWN",
            "message"       -> "Unknown Error"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with unexpected error" in {
          val eisErrorResponseBody = s"""
                                        | {
                                        |    "invalid": "error"
                                        |  }
                                        |""".stripMargin

          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponseBody))

          val response = sendRequestAndWait(url)

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNEXPECTED_ERROR",
            "message"       -> "Unexpected Error"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }

        "Bad Request if recordId is invalid" in {

          val response = sendRequestAndWait(fullUrl(s"/traders/GB123456789001/records/invalid-recordId"))

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"        -> "INVALID_QUERY_PARAMETER",
                "message"     -> "The recordId has been provided in the wrong format",
                "errorNumber" -> 25
              )
            )
          )

          verifyThatDownstreamApiWasNotCalled(hawkConnectorPath)
        }
        "Bad Request with unexpected error if error code is ot supported" in {
          stubFor(
            get(urlEqualTo(s"$hawkConnectorPath/$eori/$recordId"))
              .willReturn(
                aResponse()
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

          val response = sendRequestAndWait(url)

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

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Bad Request when no error message are found" in {
          stubFor(
            get(urlEqualTo(s"$hawkConnectorPath/$eori/$recordId"))
              .willReturn(
                aResponse()
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
                               |      "detail": ["status: fails"]
                               |    }
                               |  }
                               |}
                               |""".stripMargin)
              )
          )

          val response = sendRequestAndWait(url)

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> s"Bad Request"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Bad Request with invalid json" in {
          stubFor(
            get(urlEqualTo(s"$hawkConnectorPath/$eori/$recordId"))
              .willReturn(
                aResponse()
                  .withStatus(BAD_REQUEST)
                  .withBody(s"""
                               | {
                               |    "invalid": "error"
                               |  }
                               |""".stripMargin)
              )
          )

          val response = sendRequestAndWait(url)

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNEXPECTED_ERROR",
            "message"       -> "Unexpected Error"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
      }

      "forbidden with any of the following" - {
        "EORI number is not authorized" in {

          val response = await(
            wsClient
              .url(fullUrl(s"/traders/GB123456789015/records/$recordId"))
              .get()
          )

          response.status shouldBe FORBIDDEN
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "FORBIDDEN",
            "message"       -> s"EORI number is incorrect"
          )

          verifyThatDownstreamApiWasNotCalled(hawkConnectorPath)
        }

        "incorrect enrolment key is used to authorise " in {
          withAuthorizedTrader(enrolment = Enrolment("OTHER-ENROLMENT-KEY"))

          val response = sendRequestAndWait(url)

          response.status shouldBe FORBIDDEN
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "FORBIDDEN",
            "message"       -> s"EORI number is incorrect"
          )

          verifyThatDownstreamApiWasNotCalled(hawkConnectorPath)
        }
      }
    }
  }

  private def sendRequestAndWait(url: String) =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          ("X-Client-ID", "tss"),
          ("Accept", "application/vnd.hmrc.1.0+json")
        )
        .get()
    )

  private def stubForEis(httpStatus: Int, body: Option[String] = None) =
    stubFor(
      get(urlEqualTo(s"$hawkConnectorPath/$eori/$recordId"))
        .willReturn(
          aResponse()
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
