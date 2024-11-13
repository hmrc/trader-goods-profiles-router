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
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{AuthTestSupport, HawkIntegrationSpec}

import java.time.Instant

class PUTRecordIntegrationSpec extends HawkIntegrationSpec with AuthTestSupport with BeforeAndAfterEach {

  private val eori                       = "GB123456789001"
  private val recordId                   = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  val correlationId                      = "d677693e-9981-4ee3-8574-654981ebe606"
  val dateTime                           = "2021-12-17T09:30:47.456Z"
  val timestamp                          = "Fri, 17 Dec 2021 09:30:47 GMT"
  private val url                        = fullUrl(s"/traders/$eori/records/$recordId")
  override def hawkConnectorPath: String = "/tgp/puttgprecord/v1"

  override def beforeEach(): Unit = {
    reset(authConnector)
    withAuthorizedTrader()
    super.beforeEach()
    when(uuidService.uuid).thenReturn("d677693e-9981-4ee3-8574-654981ebe606")
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }

  "attempting to update a record, when" - {
    "the request is" - {
      "valid, specifically" - {
        "with all request fields" in {
          stubForEis(OK, Some(updateRecordEisResponseData.toString()))

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
            .futureValue

          response.status shouldBe OK
          response.json   shouldBe toJson(updateRecordResponseData.as[CreateOrUpdateRecordResponse])

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "with optional null request fields" in {
          stubForEis(
            OK,
            Some(updateEisRecordResponseDataWithOptionalFields.toString())
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestDataWithOptionalNullFields)
            .futureValue

          response.status shouldBe OK
          response.json   shouldBe toJson(updateRecordResponseDataWithOptionalFields.as[CreateOrUpdateRecordResponse])

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "with optional condition null request fields" in {
          stubForEis(
            OK,
            Some(updateEisRecordResponseDataWithConditionOptionalFields.toString())
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestDataWithConditionOptionalNullFields)
            .futureValue

          response.status shouldBe OK
          response.json   shouldBe toJson(updateRecordResponseDataWithOptionalFields.as[CreateOrUpdateRecordResponse])

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "with optional some optional null request fields" in {
          stubForEis(
            OK,
            Some(updateEisRecordResponseDataWithSomeOptionalFields.toString())
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestDataWithSomeOptionalNullFields)
            .futureValue

          response.status shouldBe OK
          response.json   shouldBe toJson(updateRecordResponseDataWithSomeOptionalFields.as[CreateOrUpdateRecordResponse])

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "with only required fields" in {
          stubForEis(OK, Some(updateRecordRequiredEisResponseData.toString()))

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequiredRequestData)
            .futureValue

          response.status shouldBe OK
          response.json   shouldBe toJson(updateRecordRequiredResponseData.as[CreateOrUpdateRecordResponse])

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
      }
      "valid but the integration call fails with response:" - {
        "Forbidden" in {
          stubForEis(FORBIDDEN)

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
            .futureValue

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

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
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
          stubForEis(BAD_GATEWAY)

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
            .futureValue

          response.status shouldBe BAD_GATEWAY
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_GATEWAY",
            "message"       -> "Bad Gateway"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Service Unavailable" in {
          stubForEis(SERVICE_UNAVAILABLE)

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
            .futureValue

          response.status shouldBe SERVICE_UNAVAILABLE
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "SERVICE_UNAVAILABLE",
            "message"       -> "Service Unavailable"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error  with 201 errorCode" in {
          stubForEis(
            INTERNAL_SERVER_ERROR,
            Some(eisErrorResponse("201", "Internal Server Error"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "INVALID_OR_EMPTY_PAYLOAD",
            "message"       -> "Invalid Response Payload or Empty payload"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error  with 401 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("401", "Unauthorised")))

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
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
            Some(eisErrorResponse("500", "Internal Server Error"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
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
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("404", "Not Found")))

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
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
            Some(eisErrorResponse("405", "Method Not Allowed"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
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
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("502", "Bad Gateway")))

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_GATEWAY",
            "message"       -> "Bad Gateway"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 503 errorCode" in {
          stubForEis(
            INTERNAL_SERVER_ERROR,
            Some(eisErrorResponse("503", "Service Unavailable"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
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
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
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
                 |      "error: 025, message: Invalid request parameter recordId",
                 |      "error: 026, message: recordId doesn’t exist in the database"
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
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
            .futureValue

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
                "message"     -> "The requested recordId doesn’t exist",
                "errorNumber" -> 26
              )
            )
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Bad Request with unexpected error" in {
          stubForEis(
            BAD_REQUEST,
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
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
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
        "Bad Request when no error list found" in {
          stubForEis(
            BAD_REQUEST,
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
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> s"Bad Request"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Bad Request with invalid json" in {
          stubForEis(
            BAD_REQUEST,
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
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
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
        "missing required request field" in {
          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(invalidRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code" -> "INVALID_REQUEST_PARAMETER",
                "message" -> "Mandatory field countryOfOrigin was missing from body or is in the wrong format",
                "errorNumber" ->13
              ),
              Json.obj(
                "code" -> "INVALID_REQUEST_PARAMETER",
                "message" -> "Mandatory field comcodeEffectiveFromDate was missing from body or is in the wrong format",
                "errorNumber" -> 23
              ),
              Json.obj(
                "code" -> "INVALID_REQUEST_PARAMETER",
                "message" -> "Mandatory field actorId was missing from body or is in the wrong format",
                "errorNumber" -> 8
              ),
              Json.obj(
                "code" -> "INVALID_REQUEST_PARAMETER",
                "message" -> "Mandatory field goodsDescription was missing from body or is in the wrong format",
                "errorNumber" -> 12
              ),
              Json.obj(
                "code" -> "INVALID_REQUEST_PARAMETER",
                "message" -> "Mandatory field comcode was missing from body or is in the wrong format",
                "errorNumber" -> 11
              ),
              Json.obj(
                "code" -> "INVALID_REQUEST_PARAMETER",
                "message" -> "Mandatory field traderRef was missing from body or is in the wrong format",
                "errorNumber" -> 9
              )
            )
          )

          verifyThatDownstreamApiWasNotCalled(hawkConnectorPath)
        }
        "for optional fields" in {
          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(invalidOptionalRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field type is in the wrong format",
                "errorNumber" -> 17
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field conditionTraderText is in the wrong format",
                "errorNumber" -> 20
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field assessmentId is in the wrong format",
                "errorNumber" -> 15
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field conditionDescription is in the wrong format",
                "errorNumber" -> 19
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field measurementUnit is in the wrong format",
                "errorNumber" -> 22
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field conditionId is in the wrong format",
                "errorNumber" -> 18
              )
            )
          )

          verifyThatDownstreamApiWasNotCalled(hawkConnectorPath)
        }
        "for optional assessment array fields" in {
          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(invalidUpdateRecordRequestDataForAssessmentArray)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field assessmentId is in the wrong format",
                "errorNumber" -> 15
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field conditionId is in the wrong format",
                "errorNumber" -> 18
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field primaryCategory is in the wrong format",
                "errorNumber" -> 16
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field type is in the wrong format",
                "errorNumber" -> 17
              )
            )
          )

          verifyThatDownstreamApiWasNotCalled(hawkConnectorPath)
        }
        "for a mandatory field actorId and an optional filed comcode" in {
          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(invalidActorIdAndComcodeRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Mandatory field comcode was missing from body or is in the wrong format",
                "errorNumber" -> 11
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Mandatory field actorId was missing from body or is in the wrong format",
                "errorNumber" -> 8
              )
            )
          )

          verifyThatDownstreamApiWasNotCalled(hawkConnectorPath)
        }
      }
      "forbidden with any of the following" - {
        "EORI number is not authorized" in {

          val response = wsClient
            .url(fullUrl(s"/traders/GB123456789015/records/$recordId"))
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
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
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .put(updateRecordRequestData)
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

  private def stubForEis(httpStatus: Int, responseBody: Option[String] = None) =
    stubFor(
      put(urlEqualTo(s"$hawkConnectorPath"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(httpStatus)
            .withBody(responseBody.orNull)
        )
    )

  val updateRecordEisResponseData: JsValue =
    Json
      .parse("""
        |{
        |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
        |  "eori": "GB123456789012",
        |  "actorId": "GB098765432112",
        |  "traderRef": "BAN001001",
        |  "comcode": "10410100",
        |  "accreditationStatus": "Withdrawn",
        |  "goodsDescription": "Organic bananas",
        |  "countryOfOrigin": "EC",
        |  "category": 1,
        |  "supplementaryUnit": 500,
        |  "measurementUnit": "Square metre (m2)",
        |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
        |  "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
        |  "version": 1,
        |  "active": true,
        |  "toReview": false,
        |  "reviewReason": "Commodity code change",
        |  "declarable": "SPIMM",
        |  "ukimsNumber": "XIUKIM47699357400020231115081800",
        |  "nirmsNumber": "RMS-GB-123456",
        |  "niphlNumber": "12345",
        |  "createdDateTime": "2024-11-18T23:20:19Z",
        |  "updatedDateTime": "2024-11-18T23:20:19Z",
        |  "assessments": [
        |    {
        |      "assessmentId": "abc123",
        |      "primaryCategory": 1,
        |      "condition": {
        |        "type": "abc123",
        |        "conditionId": "Y923",
        |        "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
        |        "conditionTraderText": "Excluded product"
        |      }
        |    }
        |  ]
        |}
        |""".stripMargin)

  val updateRecordResponseData: JsValue =
    Json
      .parse("""
               |{
               |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |  "eori": "GB123456789012",
               |  "actorId": "GB098765432112",
               |  "traderRef": "BAN001001",
               |  "comcode": "10410100",
               |  "adviceStatus": "Advice request withdrawn",
               |  "goodsDescription": "Organic bananas",
               |  "countryOfOrigin": "EC",
               |  "category": 1,
               |  "supplementaryUnit": 500,
               |  "measurementUnit": "Square metre (m2)",
               |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
               |  "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
               |  "version": 1,
               |  "active": true,
               |  "toReview": false,
               |  "reviewReason": "Commodity code change",
               |  "declarable": "SPIMM",
               |  "ukimsNumber": "XIUKIM47699357400020231115081800",
               |  "nirmsNumber": "RMS-GB-123456",
               |  "niphlNumber": "12345",
               |  "createdDateTime": "2024-11-18T23:20:19Z",
               |  "updatedDateTime": "2024-11-18T23:20:19Z",
               |  "assessments": [
               |    {
               |      "assessmentId": "abc123",
               |      "primaryCategory": 1,
               |      "condition": {
               |        "type": "abc123",
               |        "conditionId": "Y923",
               |        "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
               |        "conditionTraderText": "Excluded product"
               |      }
               |    }
               |  ]
               |}
               |""".stripMargin)

  val updateEisRecordResponseDataWithOptionalFields: JsValue =
    Json
      .parse("""
               |{
               |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |  "eori": "GB123456789012",
               |  "actorId": "GB098765432112",
               |  "traderRef": "BAN001001",
               |  "comcode": "10410100",
               |  "accreditationStatus": "Not Requested",
               |  "goodsDescription": "Organic bananas",
               |  "countryOfOrigin": "EC",
               |  "category": 1,
               |  "assessments": [
               |        {
               |            "assessmentId": null,
               |            "primaryCategory": null,
               |            "condition": {
               |                "type": null,
               |                "conditionId": null,
               |                "conditionDescription": null,
               |                "conditionTraderText": null
               |            }
               |        }
               |    ],
               |  "supplementaryUnit": 500,
               |  "measurementUnit": "Square metre (m2)",
               |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
               |  "comcodeEffectiveToDate": null,
               |  "version": 1,
               |  "active": true,
               |  "toReview": true,
               |  "reviewReason": "measure",
               |  "declarable": "SPIMM",
               |  "ukimsNumber": "XIUKIM47699357400020231115081800",
               |  "nirmsNumber": "RMS-GB-123456",
               |  "niphlNumber": "12345",
               |  "createdDateTime": "2024-11-18T23:20:19Z",
               |  "updatedDateTime": "2024-11-18T23:20:19Z"
               |}
               |""".stripMargin)

  val updateEisRecordResponseDataWithConditionOptionalFields: JsValue =
    Json
      .parse("""
               |{
               |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |  "eori": "GB123456789012",
               |  "actorId": "GB098765432112",
               |  "traderRef": "BAN001001",
               |  "comcode": "10410100",
               |  "accreditationStatus": "Not Requested",
               |  "goodsDescription": "Organic bananas",
               |  "countryOfOrigin": "EC",
               |  "category": 1,
               |  "assessments": [
               |        {
               |            "assessmentId": null,
               |            "primaryCategory": null,
               |            "condition": null
               |        }
               |    ],
               |  "supplementaryUnit": 500,
               |  "measurementUnit": "Square metre (m2)",
               |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
               |  "comcodeEffectiveToDate": null,
               |  "version": 1,
               |  "active": true,
               |  "toReview": true,
               |  "reviewReason": "measure",
               |  "declarable": "SPIMM",
               |  "ukimsNumber": "XIUKIM47699357400020231115081800",
               |  "nirmsNumber": "RMS-GB-123456",
               |  "niphlNumber": "12345",
               |  "createdDateTime": "2024-11-18T23:20:19Z",
               |  "updatedDateTime": "2024-11-18T23:20:19Z"
               |}
               |""".stripMargin)

  val updateEisRecordResponseDataWithSomeOptionalFields: JsValue =
    Json
      .parse("""
               |{
               |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |  "eori": "GB123456789012",
               |  "actorId": "GB098765432112",
               |  "traderRef": "BAN001001",
               |  "comcode": "10410100",
               |  "accreditationStatus": "Not Requested",
               |  "goodsDescription": "Organic bananas",
               |  "countryOfOrigin": "EC",
               |  "category": 1,
               |  "assessments": [
               |        {
               |            "assessmentId": null,
               |            "primaryCategory": 1
               |        }
               |    ],
               |  "supplementaryUnit": 500,
               |  "measurementUnit": "Square metre (m2)",
               |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
               |  "comcodeEffectiveToDate": null,
               |  "version": 1,
               |  "active": true,
               |  "toReview": false,
               |  "reviewReason": "Commodity code change",
               |  "declarable": "SPIMM",
               |  "ukimsNumber": "XIUKIM47699357400020231115081800",
               |  "nirmsNumber": "RMS-GB-123456",
               |  "niphlNumber": "12345",
               |  "createdDateTime": "2024-11-18T23:20:19Z",
               |  "updatedDateTime": "2024-11-18T23:20:19Z"
               |}
               |""".stripMargin)

  val updateRecordResponseDataWithOptionalFields: JsValue =
    Json
      .parse("""
               |{
               |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |  "eori": "GB123456789012",
               |  "actorId": "GB098765432112",
               |  "traderRef": "BAN001001",
               |  "comcode": "10410100",
               |  "adviceStatus": "Not Requested",
               |  "goodsDescription": "Organic bananas",
               |  "countryOfOrigin": "EC",
               |  "category": 1,
               |  "assessments": [],
               |  "supplementaryUnit": 500,
               |  "measurementUnit": "Square metre (m2)",
               |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
               |  "version": 1,
               |  "active": true,
               |  "toReview": true,
               |  "reviewReason": "measure",
               |  "declarable": "SPIMM",
               |  "ukimsNumber": "XIUKIM47699357400020231115081800",
               |  "nirmsNumber": "RMS-GB-123456",
               |  "niphlNumber": "12345",
               |  "createdDateTime": "2024-11-18T23:20:19Z",
               |  "updatedDateTime": "2024-11-18T23:20:19Z"
               |}
               |""".stripMargin)

  val updateRecordResponseDataWithSomeOptionalFields: JsValue =
    Json
      .parse("""
               |{
               |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |  "eori": "GB123456789012",
               |  "actorId": "GB098765432112",
               |  "traderRef": "BAN001001",
               |  "comcode": "10410100",
               |  "adviceStatus": "Not Requested",
               |  "goodsDescription": "Organic bananas",
               |  "countryOfOrigin": "EC",
               |  "category": 1,
               |  "assessments": [
               |        {
               |            "primaryCategory": 1
               |        }
               |    ],
               |  "supplementaryUnit": 500,
               |  "measurementUnit": "Square metre (m2)",
               |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
               |  "version": 1,
               |  "active": true,
               |  "toReview": false,
               |  "reviewReason": "Commodity code change",
               |  "declarable": "SPIMM",
               |  "ukimsNumber": "XIUKIM47699357400020231115081800",
               |  "nirmsNumber": "RMS-GB-123456",
               |  "niphlNumber": "12345",
               |  "createdDateTime": "2024-11-18T23:20:19Z",
               |  "updatedDateTime": "2024-11-18T23:20:19Z"
               |}
               |""".stripMargin)

  val updateRecordRequestData: String =
    s"""
        |{
        |    "eori": "$eori",
        |    "actorId": "GB098765432112",
        |    "traderRef": "BAN001001",
        |    "comcode": "10410100",
        |    "goodsDescription": "Organic bananas",
        |    "countryOfOrigin": "EC",
        |    "category": 1,
        |    "assessments": [
        |        {
        |            "assessmentId": "abc123",
        |            "primaryCategory": 1,
        |            "condition": {
        |                "type": "abc123",
        |                "conditionId": "Y923",
        |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
        |                "conditionTraderText": "Excluded product"
        |            }
        |        }
        |    ],
        |    "supplementaryUnit": 500,
        |    "measurementUnit": "Square metre (m2)",
        |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
        |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
        |}
        |""".stripMargin

  val updateEisRecordRequestData: String =
    """
      |{
      |    "eori": "GB123456789001",
      |    "actorId": "GB098765432112",
      |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
      |    "traderRef": "BAN001001",
      |    "comcode": "10410100",
      |    "goodsDescription": "Organic bananas",
      |    "countryOfOrigin": "EC",
      |    "category": 1,
      |    "assessments": [],
      |    "supplementaryUnit": 500,
      |    "measurementUnit": "Square metre (m2)",
      |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z"
      |}
      |""".stripMargin

  val updateEisRecordRequestDataWithSomeOptionalFields: String =
    """
      |{
      |    "eori": "GB123456789001",
      |    "actorId": "GB098765432112",
      |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
      |    "traderRef": "BAN001001",
      |    "comcode": "10410100",
      |    "goodsDescription": "Organic bananas",
      |    "countryOfOrigin": "EC",
      |    "category": 1,
      |    "assessments": [
      |        {
      |            "primaryCategory": 1
      |        }
      |    ],
      |    "supplementaryUnit": 500,
      |    "measurementUnit": "Square metre (m2)",
      |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z"
      |}
      |""".stripMargin

  val updateRecordRequestDataWithOptionalNullFields: String =
    """
      |{
      |    "eori": "GB123456789001",
      |    "actorId": "GB098765432112",
      |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
      |    "traderRef": "BAN001001",
      |    "comcode": "10410100",
      |    "goodsDescription": "Organic bananas",
      |    "countryOfOrigin": "EC",
      |    "category": 1,
      |    "assessments": [
      |        {
      |            "assessmentId": null,
      |            "primaryCategory": null,
      |            "condition": {
      |                "type": null,
      |                "conditionId": null,
      |                "conditionDescription": null,
      |                "conditionTraderText": null
      |            }
      |        }
      |    ],
      |    "supplementaryUnit": 500,
      |    "measurementUnit": "Square metre (m2)",
      |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
      |    "comcodeEffectiveToDate": null
      |}
      |""".stripMargin

  val updateRecordRequestDataWithConditionOptionalNullFields: String =
    """
      |{
      |    "eori": "GB123456789001",
      |    "actorId": "GB098765432112",
      |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
      |    "traderRef": "BAN001001",
      |    "comcode": "10410100",
      |    "goodsDescription": "Organic bananas",
      |    "countryOfOrigin": "EC",
      |    "category": 1,
      |    "assessments": [
      |        {
      |            "assessmentId": null,
      |            "primaryCategory": null,
      |            "condition": null
      |        }
      |    ],
      |    "supplementaryUnit": 500,
      |    "measurementUnit": "Square metre (m2)",
      |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
      |    "comcodeEffectiveToDate": null
      |}
      |""".stripMargin

  val updateRecordRequestDataWithSomeOptionalNullFields: String =
    """
      |{
      |    "eori": "GB123456789001",
      |    "actorId": "GB098765432112",
      |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
      |    "traderRef": "BAN001001",
      |    "comcode": "10410100",
      |    "goodsDescription": "Organic bananas",
      |    "countryOfOrigin": "EC",
      |    "category": 1,
      |    "assessments": [
      |        {
      |            "assessmentId": null,
      |            "primaryCategory": 1
      |        }
      |    ],
      |    "supplementaryUnit": 500,
      |    "measurementUnit": "Square metre (m2)",
      |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
      |    "comcodeEffectiveToDate": null
      |}
      |""".stripMargin

  val updateRecordRequiredRequestData: String =
    """
      |{
      |    "eori": "GB123456789012",
      |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
      |    "actorId": "GB098765432112",
      |    "traderRef": "BAN001001",
      |    "comcode": "10410100",
      |    "goodsDescription": "Organic bananas",
      |    "countryOfOrigin": "EC",
      |    "category": 1,
      |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z"
      |}
      |""".stripMargin

  val updateRecordRequiredEisResponseData: JsValue =
    Json
      .parse("""
          |{
          |    "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
          |    "eori": "GB123456789012",
          |    "actorId": "GB098765432112",
          |    "traderRef": "BAN001001",
          |    "comcode": "10410100",
          |    "accreditationStatus": "Not Requested",
          |    "goodsDescription": "Organic bananas",
          |    "countryOfOrigin": "EC",
          |    "category": 1,
          |    "assessments": null,
          |    "supplementaryUnit": null,
          |    "measurementUnit": null,
          |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
          |    "comcodeEffectiveToDate": null,
          |    "version": 1,
          |    "active": true,
          |    "toReview": true,
          |    "reviewReason": "unclear",
          |    "declarable": "SPIMM",
          |    "ukimsNumber": "XIUKIM47699357400020231115081800",
          |    "nirmsNumber": "RMS-GB-123456",
          |    "niphlNumber": "12345",
          |    "createdDateTime": "2024-11-18T23:20:19Z",
          |    "updatedDateTime": "2024-11-18T23:20:19Z"
          |}
          |""".stripMargin)

  val updateRecordRequiredResponseData: JsValue =
    Json
      .parse("""
               |{
               |    "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |    "eori": "GB123456789012",
               |    "actorId": "GB098765432112",
               |    "traderRef": "BAN001001",
               |    "comcode": "10410100",
               |    "adviceStatus": "Not Requested",
               |    "goodsDescription": "Organic bananas",
               |    "countryOfOrigin": "EC",
               |    "category": 1,
               |    "assessments": null,
               |    "supplementaryUnit": null,
               |    "measurementUnit": null,
               |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
               |    "comcodeEffectiveToDate": null,
               |    "version": 1,
               |    "active": true,
               |    "toReview": true,
               |    "reviewReason": "unclear",
               |    "declarable": "SPIMM",
               |    "ukimsNumber": "XIUKIM47699357400020231115081800",
               |    "nirmsNumber": "RMS-GB-123456",
               |    "niphlNumber": "12345",
               |    "createdDateTime": "2024-11-18T23:20:19Z",
               |    "updatedDateTime": "2024-11-18T23:20:19Z"
               |}
               |""".stripMargin)

  val invalidRequestData: String =
    """
      |{
      |    "assessments": [
      |        {
      |            "assessmentId": "abc123",
      |            "primaryCategory": 1,
      |            "condition": {
      |                "type": "abc123",
      |                "conditionId": "Y923",
      |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
      |                "conditionTraderText": "Excluded product"
      |            }
      |        }
      |    ],
      |    "supplementaryUnit": 500,
      |    "measurementUnit": "Square metre (m2)",
      |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
      |}
      |""".stripMargin

  val invalidOptionalRequestData: String =
    """
      |{
      |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
      |  "eori": "GB123456789012",
      |    "actorId": "GB098765432112",
      |    "traderRef": "BAN001001",
      |    "comcode": "10410100",
      |    "goodsDescription": "Organic bananas",
      |    "countryOfOrigin": "EC",
      |    "category": 2,
      |    "assessments": [
      |        {
      |            "assessmentId": "",
      |            "primaryCategory": 1,
      |            "condition": {
      |                "type": "",
      |                "conditionId": "",
      |                "conditionDescription": "",
      |                "conditionTraderText": ""
      |            }
      |        }
      |    ],
      |    "supplementaryUnit": 500,
      |    "measurementUnit": "",
      |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
      |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
      |}
      |""".stripMargin

  val invalidUpdateRecordRequestDataForAssessmentArray: JsValue = Json
    .parse("""
             |{
             |    "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
             |    "actorId": "GB098765432112",
             |    "traderRef": "BAN001001",
             |    "comcode": "10410100",
             |    "goodsDescription": "Organic bananas",
             |    "countryOfOrigin": "EC",
             |    "category": 1,
             |    "assessments": [
             |        {
             |            "assessmentId": "abc123",
             |            "primaryCategory": 1,
             |            "condition": {
             |                "type": "abc123",
             |                "conditionId": "Y923",
             |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
             |                "conditionTraderText": "Excluded product"
             |            }
             |        },
             |        {
             |            "assessmentId": "",
             |            "primaryCategory": "test",
             |            "condition": {
             |                "type": "",
             |                "conditionId": "",
             |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
             |                "conditionTraderText": "Excluded product"
             |            }
             |        }
             |    ],
             |    "supplementaryUnit": 500,
             |    "measurementUnit": "Square metre (m2)",
             |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
             |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
             |}
             |""".stripMargin)

  val invalidActorIdAndComcodeRequestData: String =
    """
      |{
      |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
      |  "eori": "GB123456789012",
      |    "actorId": "GB12",
      |    "traderRef": "BAN001001",
      |    "comcode": "104101000",
      |    "goodsDescription": "Organic bananas",
      |    "countryOfOrigin": "EC",
      |    "category": 2,
      |    "assessments": [
      |        {
      |            "assessmentId": "abc123",
      |            "primaryCategory": 1,
      |            "condition": {
      |                "type": "abc123",
      |                "conditionId": "Y923",
      |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
      |                "conditionTraderText": "Excluded product"
      |            }
      |        }
      |    ],
      |    "supplementaryUnit": 500,
      |    "measurementUnit": "Square metre (m2)",
      |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
      |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
      |}
      |""".stripMargin

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
}
