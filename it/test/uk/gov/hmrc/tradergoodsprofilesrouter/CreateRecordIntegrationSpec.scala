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
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{AuthTestSupport, HawkIntegrationSpec}

import java.time.Instant

class CreateRecordIntegrationSpec extends HawkIntegrationSpec with AuthTestSupport with BeforeAndAfterEach {

  private val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  private val url           = fullUrl("/traders/GB123456789001/records")

  override def hawkConnectorPath: String = "/tgp/createrecord/v1"

  override def beforeEach(): Unit = {
    reset(authConnector)
    withAuthorizedTrader()
    super.beforeEach()
    when(uuidService.uuid).thenReturn("d677693e-9981-4ee3-8574-654981ebe606")
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }

  "attempting to create a record, when" - {
    "the request is" - {
      "valid, specifically" - {
        "with all request fields" in {
          stubForEis(CREATED, Some(createRecordEisResponseData.toString()))

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe CREATED
          response.json   shouldBe toJson(
            createRecordResponseData.as[CreateOrUpdateRecordResponse]
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "with optional null request fields" in {
          stubForEis(CREATED, Some(createEisRecordResponseDataWithOptionalNullFields.toString()))

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .post(createRecordRequestDataWithOptionalNullFields)
            .futureValue

          response.status shouldBe CREATED
          response.json   shouldBe toJson(
            createRecordResponseDataWithOptionalNullFields.as[CreateOrUpdateRecordResponse]
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "with optional condition null request fields" in {
          stubForEis(CREATED, Some(createEisRecordResponseDataWithConditionOptionalNullFields.toString()))

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .post(createRecordRequestDataWithConditionOptionalNullFields)
            .futureValue

          response.status shouldBe CREATED
          response.json   shouldBe toJson(
            createRecordResponseDataWithOptionalNullFields.as[CreateOrUpdateRecordResponse]
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "with optional some optional null request fields" in {
          stubForEis(CREATED, Some(createEisRecordResponseDataWithSomeOptionalNullFields.toString()))

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .post(createRecordRequestDataWithSomeOptionalNullFields)
            .futureValue

          response.status shouldBe CREATED
          response.json   shouldBe toJson(
            createRecordResponseDataWithSomeOptionalNullFields.as[CreateOrUpdateRecordResponse]
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "with only required fields" in {
          stubForEis(CREATED, Some(createRecordRequiredEisResponseData.toString()))

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .post(createRecordRequiredRequestData)
            .futureValue

          response.status shouldBe CREATED
          response.json   shouldBe toJson(createRecordRequiredResponseData.as[CreateOrUpdateRecordResponse])

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
            .post(createRecordRequestData)
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
            .post(createRecordRequestData)
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
            .post(createRecordRequestData)
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
            .post(createRecordRequestData)
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
            .post(createRecordRequestData)
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
            .post(createRecordRequestData)
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
            .post(createRecordRequestData)
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
            .post(createRecordRequestData)
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
            .post(createRecordRequestData)
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
            .post(createRecordRequestData)
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
            .post(createRecordRequestData)
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
            .post(createRecordRequestData)
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
            .post(createRecordRequestData)
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
                "message"     -> "The requested recordId to update doesn’t exist",
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
            .post(createRecordRequestData)
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
        "Bad Request with unable to parse the detail" in {
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
            .post(createRecordRequestData)
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
            .post(createRecordRequestData)
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
//        "missing required header" in {
//          val response = sendRequestAndWait(url)
//
//          response.status shouldBe BAD_REQUEST
//          response.json   shouldBe Json.obj(
//            "correlationId" -> correlationId,
//            "code"          -> "BAD_REQUEST",
//            "message"       -> "Bad Request",
//            "errors"        -> Json.arr(
//              Json.obj(
//                "code"        -> "INVALID_HEADER",
//                "message"     -> "Missing mandatory header X-Client-ID",
//                "errorNumber" -> 6000
//              )
//            )
//          )
//
//          verifyThatDownstreamApiWasNotCalled(hawkConnectorPath)
//        }
        "missing required request field" in {
          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .post(invalidRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Mandatory field actorId was missing from body or is in the wrong format",
                "errorNumber" -> 8
              )
            )
          )

          verifyThatDownstreamApiWasNotCalled(hawkConnectorPath)
        }
        "category field is out of range" in {
          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .post(invalidCategoryRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Mandatory field category was missing from body or is in the wrong format",
                "errorNumber" -> 14
              )
            )
          )

          verifyThatDownstreamApiWasNotCalled(hawkConnectorPath)
        }
        "missing required field from assessment" in {
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
                    |        "error: 017, message: Optional field type is in the wrong format",
                    |        "error: 015, message: Optional field assessmentId is in the wrong format",
                    |        "error: 016, message: Optional field primaryCategory is in the wrong format",
                    |        "error: 018, message: Optional field conditionId is in the wrong format",
                    |        "error: 008, message: Mandatory field actorId was missing from body or is in the wrong format"
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
            .post(invalidCreateRecordRequestDataForAssessmentArray)
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
                "message"     -> "Optional field assessmentId is in the wrong format",
                "errorNumber" -> 15
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field primaryCategory is in the wrong format",
                "errorNumber" -> 16
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field conditionId is in the wrong format",
                "errorNumber" -> 18
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
        "supplementaryUnit is out of range" in {
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
                    |        "error: 021, message: Optional field supplementaryUnit is in the wrong format"
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
            .post(outOfRangeSupplementaryUnitRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field supplementaryUnit is in the wrong format",
                "errorNumber" -> 21
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
            .post(invalidOptionalRequestData)
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
            .post(invalidCreateRecordRequestDataForAssessmentArray)
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
                "message"     -> "Optional field assessmentId is in the wrong format",
                "errorNumber" -> 15
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field primaryCategory is in the wrong format",
                "errorNumber" -> 16
              ),
              Json.obj(
                "code"        -> "INVALID_REQUEST_PARAMETER",
                "message"     -> "Optional field conditionId is in the wrong format",
                "errorNumber" -> 18
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
        "for mandatory fields actorId and comcode" in {
          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/vnd.hmrc.1.0+json"),
              ("X-Client-ID", "tss")
            )
            .post(invalidActorIdAndComcodeRequestData)
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
        "forbidden with any of the following" - {
          "EORI number is not authorized" in {

            val response = wsClient
              .url(fullUrl("/traders/GB123456789015/records"))
              .withHttpHeaders(
                ("Content-Type", "application/json"),
                ("Accept", "application/vnd.hmrc.1.0+json"),
                ("X-Client-ID", "tss")
              )
              .post(createRecordRequestData)
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
              .post(createRecordRequestData)
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
  }

  private def stubForEis(httpStatus: Int, responseBody: Option[String] = None) =
    stubFor(
      post(urlEqualTo(s"$hawkConnectorPath"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(httpStatus)
            .withBody(responseBody.orNull)
        )
    )

  val createRecordEisResponseData: JsValue =
    Json
      .parse("""
        |{
        |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
        |  "eori": "GB123456789001",
        |  "actorId": "GB098765432112",
        |  "traderRef": "BAN001001",
        |  "comcode": "10410100",
        |  "accreditationStatus": "Not Requested",
        |  "goodsDescription": "Organic bananas",
        |  "countryOfOrigin": "EC",
        |  "category": 1,
        |  "supplementaryUnit": 500,
        |  "measurementUnit": "Square metre (m2)",
        |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
        |  "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
        |  "version": 1,
        |  "active": true,
        |  "toReview": true,
        |  "reviewReason": "commodity",
        |  "declarable": "SPIMM",
        |  "ukimsNumber": "XIUKIM47699357400020231115081800",
        |  "nirmsNumber": "RMS-GB-123456",
        |  "niphlNumber": "6 S12345",
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

  val createRecordResponseData: JsValue =
    Json
      .parse("""
               |{
               |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |  "eori": "GB123456789001",
               |  "actorId": "GB098765432112",
               |  "traderRef": "BAN001001",
               |  "comcode": "10410100",
               |  "adviceStatus": "Not Requested",
               |  "goodsDescription": "Organic bananas",
               |  "countryOfOrigin": "EC",
               |  "category": 1,
               |  "supplementaryUnit": 500,
               |  "measurementUnit": "Square metre (m2)",
               |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
               |  "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
               |  "version": 1,
               |  "active": true,
               |  "toReview": true,
               |  "reviewReason": "The commodity code has expired. You'll need to change the commodity code and categorise the goods.",
               |  "declarable": "SPIMM",
               |  "ukimsNumber": "XIUKIM47699357400020231115081800",
               |  "nirmsNumber": "RMS-GB-123456",
               |  "niphlNumber": "6 S12345",
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

  val createEisRecordResponseDataWithOptionalNullFields: JsValue =
    Json
      .parse("""
               |{
               |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |  "eori": "GB123456789001",
               |  "actorId": "GB098765432112",
               |  "traderRef": "BAN001001",
               |  "comcode": "10410100",
               |  "accreditationStatus": "Not Requested",
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
               |  "niphlNumber": "6 S12345",
               |  "createdDateTime": "2024-11-18T23:20:19Z",
               |  "updatedDateTime": "2024-11-18T23:20:19Z",
               |  "assessments": [
               |    {
               |      "assessmentId": null,
               |      "primaryCategory": null,
               |      "condition": {
               |        "type": null,
               |        "conditionId": null,
               |        "conditionDescription": null,
               |        "conditionTraderText": null
               |      }
               |    }
               |  ]
               |}
               |""".stripMargin)

  val createEisRecordResponseDataWithConditionOptionalNullFields: JsValue =
    Json
      .parse("""
               |{
               |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |  "eori": "GB123456789001",
               |  "actorId": "GB098765432112",
               |  "traderRef": "BAN001001",
               |  "comcode": "10410100",
               |  "accreditationStatus": "Not Requested",
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
               |  "niphlNumber": "6 S12345",
               |  "createdDateTime": "2024-11-18T23:20:19Z",
               |  "updatedDateTime": "2024-11-18T23:20:19Z",
               |  "assessments": [
               |    {
               |      "assessmentId": null,
               |      "primaryCategory": null,
               |      "condition": null
               |    }
               |  ]
               |}
               |""".stripMargin)

  val createEisRecordResponseDataWithSomeOptionalNullFields: JsValue =
    Json
      .parse("""
               |{
               |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |  "eori": "GB123456789001",
               |  "actorId": "GB098765432112",
               |  "traderRef": "BAN001001",
               |  "comcode": "10410100",
               |  "accreditationStatus": "Not Requested",
               |  "goodsDescription": "Organic bananas",
               |  "countryOfOrigin": "EC",
               |  "category": 1,
               |  "supplementaryUnit": 500,
               |  "measurementUnit": "Square metre (m2)",
               |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
               |  "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
               |  "version": 1,
               |  "active": true,
               |  "toReview": true,
               |  "reviewReason": "mismatch",
               |  "declarable": "SPIMM",
               |  "ukimsNumber": "XIUKIM47699357400020231115081800",
               |  "nirmsNumber": "RMS-GB-123456",
               |  "niphlNumber": "6 S12345",
               |  "createdDateTime": "2024-11-18T23:20:19Z",
               |  "updatedDateTime": "2024-11-18T23:20:19Z",
               |  "assessments": [
               |    {
               |      "assessmentId": null,
               |      "primaryCategory": 1
               |    }
               |  ]
               |}
               |""".stripMargin)

  val createRecordResponseDataWithOptionalNullFields: JsValue =
    Json
      .parse("""
               |{
               |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |  "eori": "GB123456789001",
               |  "actorId": "GB098765432112",
               |  "traderRef": "BAN001001",
               |  "comcode": "10410100",
               |  "adviceStatus": "Not Requested",
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
               |  "declarable": "SPIMM",
               |  "ukimsNumber": "XIUKIM47699357400020231115081800",
               |  "nirmsNumber": "RMS-GB-123456",
               |  "niphlNumber": "6 S12345",
               |  "createdDateTime": "2024-11-18T23:20:19Z",
               |  "updatedDateTime": "2024-11-18T23:20:19Z",
               |  "assessments": []
               |}
               |""".stripMargin)

  val createRecordResponseDataWithSomeOptionalNullFields: JsValue =
    Json
      .parse("""
               |{
               |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |  "eori": "GB123456789001",
               |  "actorId": "GB098765432112",
               |  "traderRef": "BAN001001",
               |  "comcode": "10410100",
               |  "adviceStatus": "Not Requested",
               |  "goodsDescription": "Organic bananas",
               |  "countryOfOrigin": "EC",
               |  "category": 1,
               |  "supplementaryUnit": 500,
               |  "measurementUnit": "Square metre (m2)",
               |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
               |  "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
               |  "version": 1,
               |  "active": true,
               |  "toReview": true,
               |  "reviewReason": "HMRC have reviewed this record. The commodity code and goods description do not match. If you want to use this record on an IMMI, you'll need to amend the commodity code and the goods description.",
               |  "declarable": "SPIMM",
               |  "ukimsNumber": "XIUKIM47699357400020231115081800",
               |  "nirmsNumber": "RMS-GB-123456",
               |  "niphlNumber": "6 S12345",
               |  "createdDateTime": "2024-11-18T23:20:19Z",
               |  "updatedDateTime": "2024-11-18T23:20:19Z",
               "assessments": [
               |    {
               |      "primaryCategory": 1
               |    }
               |  ]
               |}
               |""".stripMargin)

  val createRecordRequestData: String =
    """
        |{
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

  val createRecordRequestDataWithOptionalNullFields: String =
    """
      |{
      |    "actorId": "GB098765432112",
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
      |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
      |}
      |""".stripMargin

  val createRecordRequestDataWithConditionOptionalNullFields: String =
    """
      |{
      |    "actorId": "GB098765432112",
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
      |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
      |}
      |""".stripMargin

  val createRecordRequestDataWithSomeOptionalNullFields: String =
    """
      |{
      |    "actorId": "GB098765432112",
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
      |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
      |}
      |""".stripMargin

  val createRecordRequiredRequestData: String =
    """
      |{
      |    "eori": "GB123456789001",
      |    "actorId": "GB098765432112",
      |    "traderRef": "BAN001001",
      |    "comcode": "10410100",
      |    "goodsDescription": "Organic bananas",
      |    "countryOfOrigin": "EC",
      |    "category": 1,
      |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z"
      |}
      |""".stripMargin

  val createRecordRequiredEisResponseData: JsValue =
    Json
      .parse("""
          |{
          |    "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
          |    "eori": "GB123456789001",
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
          |    "reviewReason": "inadequate",
          |    "declarable": "SPIMM",
          |    "ukimsNumber": "XIUKIM47699357400020231115081800",
          |    "nirmsNumber": "RMS-GB-123456",
          |    "niphlNumber": "6 S12345",
          |    "createdDateTime": "2024-11-18T23:20:19Z",
          |    "updatedDateTime": "2024-11-18T23:20:19Z"
          |}
          |""".stripMargin)

  val createRecordRequiredResponseData: JsValue =
    Json
      .parse("""
               |{
               |    "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |    "eori": "GB123456789001",
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
               |    "reviewReason":  "HMRC have reviewed this record. The goods description does not have enough detail. If you want to use this record on an IMMI, you'll need to amend the goods description",
               |    "declarable": "SPIMM",
               |    "ukimsNumber": "XIUKIM47699357400020231115081800",
               |    "nirmsNumber": "RMS-GB-123456",
               |    "niphlNumber": "6 S12345",
               |    "createdDateTime": "2024-11-18T23:20:19Z",
               |    "updatedDateTime": "2024-11-18T23:20:19Z"
               |}
               |""".stripMargin)

  val invalidRequestData: String =
    """
      |{    
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

  val invalidCategoryRequestData: String =
    """
      |{
      |  "eori": "GB123456789001",
      |    "actorId": "GB098765432112",
      |    "traderRef": "BAN001001",
      |    "comcode": "10410100",
      |    "goodsDescription": "Organic bananas",
      |    "countryOfOrigin": "EC",
      |    "category": 24,
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

  val invalidOptionalRequestData: String =
    """
      |{
      |  "eori": "GB123456789001",
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

  val invalidCreateRecordRequestDataForAssessmentArray: JsValue = Json
    .parse("""
             |{
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
      |  "eori": "GB123456789001",
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

  val outOfRangeSupplementaryUnitRequestData: JsValue = Json
    .parse("""
             |{
             |    "eori": "GB123456789012",
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
             |    "supplementaryUnit": "25Kg",
             |    "measurementUnit": "Square metre (m2)",
             |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
             |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
             |}
             |""".stripMargin)

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

  private def sendRequestAndWait(url: String) =
    // TODO: After Drop 1.1 this should be removed and use the request without the X-CLient-ID header -  Ticket: TGP-2014
    if (appConfig.isDrop1_1_enabled)
      await(
        wsClient
          .url(url)
          .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/vnd.hmrc.1.0+json"))
          .post(createRecordRequestData)
      )
    else
      await(
        wsClient
          .url(url)
          .withHttpHeaders(
            ("X-Client-ID", "tss"),
            ("Content-Type", "application/json"),
            ("Accept", "application/vnd.hmrc.1.0+json")
          )
          .post(createRecordRequestData)
      )
}
