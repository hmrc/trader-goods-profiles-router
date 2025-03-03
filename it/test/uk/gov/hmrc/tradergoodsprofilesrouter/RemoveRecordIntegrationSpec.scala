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
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{AuthTestSupport, HawkIntegrationSpec}

import java.time.Instant

class RemoveRecordIntegrationSpec extends HawkIntegrationSpec with AuthTestSupport with BeforeAndAfterEach {

  private val eori                       = "GB123456789001"
  private val actorId                    = "GB123456789001"
  private val recordId                   = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  private val correlationId              = "d677693e-9981-4ee3-8574-654981ebe606"
  private val dateTime                   = "2021-12-17T09:30:47.456Z"
  private val timestamp                  = "Fri, 17 Dec 2021 09:30:47 GMT"
  private val url                        = fullUrl(s"/traders/$eori/records/$recordId?actorId=$actorId")
  override def hawkConnectorPath: String = s"/tgp/removerecord/v1"

  override def beforeEach(): Unit = {
    reset(authConnector)
    withAuthorizedTrader()
    super.beforeEach()
    when(uuidService.uuid).thenReturn(correlationId)
    when(dateTimeService.timestamp).thenReturn(Instant.parse(dateTime))

  }

  "attempting to remove record, when" - {
    "the request is" - {
      "valid" in {
        stubForEis(OK, removeEisRecordRequest)

        val response = wsClient
          .url(url)
          .withHttpHeaders(
            ("Content-Type", "application/json"),
            ("X-Client-ID", "tss"),
            ("Accept", "application/vnd.hmrc.1.0+json")
          )
          .delete()
          .futureValue

        response.status shouldBe NO_CONTENT

        verifyThatDownstreamApiWasCalled(hawkConnectorPath)
      }

      "valid but the integration call fails with response:" - {
        "Forbidden" in {
          stubForEis(FORBIDDEN, removeEisRecordRequest)

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(
                ("Content-Type", "application/json"),
                ("X-Client-ID", "tss"),
                ("Accept", "application/vnd.hmrc.1.0+json")
              )
              .delete()
          )

          response.status shouldBe FORBIDDEN
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "FORBIDDEN",
            "message"       -> "Forbidden"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Not Found" in {
          stubForEis(NOT_FOUND, removeEisRecordRequest)

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

          response.status shouldBe NOT_FOUND
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "NOT_FOUND",
            "message"       -> "Not Found"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Bad Gateway" in {
          stubForEis(BAD_GATEWAY, removeEisRecordRequest)

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

          response.status shouldBe BAD_GATEWAY
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_GATEWAY",
            "message"       -> "Bad Gateway"
          )

          verifyThatDownstreamApiWasRetried(hawkConnectorPath)
        }
        "Service Unavailable" in {
          stubForEis(SERVICE_UNAVAILABLE, removeEisRecordRequest)

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

          response.status shouldBe SERVICE_UNAVAILABLE
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "SERVICE_UNAVAILABLE",
            "message"       -> "Service Unavailable"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error  with 401 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, removeEisRecordRequest, Some(eisErrorResponse("401", "Unauthorised")))

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNAUTHORIZED",
            "message"       -> "Unauthorized"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error  with 500 errorCode" in {
          stubForEis(
            INTERNAL_SERVER_ERROR,
            removeEisRecordRequest,
            Some(eisErrorResponse("500", "Internal Server Error"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "INTERNAL_SERVER_ERROR",
            "message"       -> "Internal Server Error"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 404 errorCode" in {
          stubForEis(
            INTERNAL_SERVER_ERROR,
            removeEisRecordRequest,
            Some(eisErrorResponse("404", "Not Found"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "NOT_FOUND",
            "message"       -> "Not Found"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 405 errorCode" in {
          stubForEis(
            INTERNAL_SERVER_ERROR,
            removeEisRecordRequest,
            Some(eisErrorResponse("405", "Method Not Allowed"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "METHOD_NOT_ALLOWED",
            "message"       -> "Method Not Allowed"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 502 errorCode" in {
          stubForEis(
            INTERNAL_SERVER_ERROR,
            removeEisRecordRequest,
            Some(eisErrorResponse("502", "Bad Gateway"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_GATEWAY",
            "message"       -> "Bad Gateway"
          )

          verifyThatDownstreamApiWasRetried(hawkConnectorPath)
        }
        "Internal Server Error with 503 errorCode" in {
          stubForEis(
            INTERNAL_SERVER_ERROR,
            removeEisRecordRequest,
            Some(eisErrorResponse("503", "Service Unavailable"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "SERVICE_UNAVAILABLE",
            "message"       -> "Service Unavailable"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Bad Request with one error detail" in {
          stubForEis(
            BAD_REQUEST,
            removeEisRecordRequest,
            Some(s"""
                    |{
                    |  "errorDetail": {
                    |    "timestamp": "2023-09-14T11:29:18Z",
                    |    "correlationId": "$correlationId",
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

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

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

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Bad Request with more than one error details" in {
          stubForEis(
            BAD_REQUEST,
            removeEisRecordRequest,
            Some(s"""
                    |{
                    |  "errorDetail": {
                    |    "timestamp": "2023-09-14T11:29:18Z",
                    |    "correlationId": "$correlationId",
                    |    "errorCode": "400",
                    |    "errorMessage": "Invalid request parameter",
                    |    "source": "BACKEND",
                    |    "sourceFaultDetail": {
                    |      "detail": [
                    |        "error: 006, message: Mandatory field eori was missing from body",
                    |        "error: 008, message: Mandatory field actorId was missing from body",
                    |        "error: 025, message: The recordId has been provided in the wrong format",
                    |        "error: 027, message: There is an ongoing accreditation request and the record can not be updated"
                    |      ]
                    |    }
                    |  }
                    |}
                    |""".stripMargin)
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

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
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Mandatory field actorId was missing from body or is in the wrong format",
                "errorNumber" -> 8
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "The recordId has been provided in the wrong format",
                "errorNumber" -> 25
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "There is an ongoing accreditation request and the record can not be updated",
                "errorNumber" -> 27
              )
            )
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Bad Request with unexpected error" in {
          stubForEis(
            BAD_REQUEST,
            removeEisRecordRequest,
            Some(s"""
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

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

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
          stubForEis(
            BAD_REQUEST,
            removeEisRecordRequest,
            Some(s"""
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

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Bad Request with invalid json" in {
          stubForEis(
            BAD_REQUEST,
            removeEisRecordRequest,
            Some(s"""
                    | {
                    |    "invalid": "error"
                    |  }
                    |""".stripMargin)
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNEXPECTED_ERROR",
            "message"       -> "Unexpected Error"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
      }
      "invalid, specifically" - {
        "missing required header" in {
          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"))
            .delete()
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"        -> "INVALID_HEADER",
                "message"     -> "Missing mandatory header X-Client-ID",
                "errorNumber" -> 6000
              )
            )
          )

          verifyThatDownstreamApiWasNotCalled(hawkConnectorPath)
        }
      }
      "forbidden with any of the following" - {
        "EORI number is not authorized" in {

          val response = wsClient
            .url(fullUrl(s"/traders/GB123456789015/records/$recordId?actorId=$actorId"))
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

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

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("X-Client-ID", "tss"),
              ("Accept", "application/vnd.hmrc.1.0+json")
            )
            .delete()
            .futureValue

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

  private def stubForEis(httpStatus: Int, requestBody: String, responseBody: Option[String] = None) =
    stubFor(
      put(urlEqualTo(s"$hawkConnectorPath"))
        .withRequestBody(equalToJson(requestBody))
        .withHeader("Content-Type", equalTo("application/json"))
        .withHeader("X-Forwarded-Host", equalTo("MDTP"))
        .withHeader("X-Correlation-ID", equalTo(correlationId))
        .withHeader("Date", equalTo(timestamp))
        .withHeader("Accept", equalTo("application/json"))
        .withHeader("Authorization", equalTo("Bearer c29tZS10b2tlbgo="))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(httpStatus)
            .withBody(responseBody.orNull)
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

  lazy val removeEisRecordRequest: String =
    """
        |{
        |  "eori": "GB123456789001",
        |  "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        |  "actorId": "GB123456789001"
        |}
        |""".stripMargin

}
