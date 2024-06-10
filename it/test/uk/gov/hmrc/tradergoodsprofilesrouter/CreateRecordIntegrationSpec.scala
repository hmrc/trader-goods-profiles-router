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
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse

import java.time.Instant

class CreateRecordIntegrationSpec extends BaseIntegrationWithConnectorSpec with BeforeAndAfterEach {

  private val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  private val url           = fullUrl("/traders/GB123456789012/records")

  override def connectorPath: String = "/tgp/createrecord/v1"
  override def connectorName: String = "eis"

  override def beforeEach: Unit = {
    super.beforeEach()
    when(uuidService.uuid).thenReturn("d677693e-9981-4ee3-8574-654981ebe606")
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }

  "attempting to create a record, when" - {
    "the request is" - {
      "valid, specifically" - {
        "with all request fields" in {
          stubForEis(CREATED, Some(createRecordResponseData.toString()))

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe CREATED
          response.json   shouldBe toJson(
            createRecordResponseData.as[CreateOrUpdateRecordResponse]
          )

          verifyThatDownstreamApiWasCalled()
        }
        "with optional null request fields" in {
          stubForEis(CREATED, Some(createEisRecordResponseDataWithOptionalNullFields.toString()))

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestDataWithOptionalNullFields)
            .futureValue

          response.status shouldBe CREATED
          response.json   shouldBe toJson(
            createRecordResponseDataWithOptionalNullFields.as[CreateOrUpdateRecordResponse]
          )

          verifyThatDownstreamApiWasCalled()
        }
        "with optional condition null request fields" in {
          stubForEis(CREATED, Some(createEisRecordResponseDataWithConditionOptionalNullFields.toString()))

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestDataWithConditionOptionalNullFields)
            .futureValue

          response.status shouldBe CREATED
          response.json   shouldBe toJson(
            createRecordResponseDataWithOptionalNullFields.as[CreateOrUpdateRecordResponse]
          )

          verifyThatDownstreamApiWasCalled()
        }
        "with optional some optional null request fields" in {
          stubForEis(CREATED, Some(createEisRecordResponseDataWithSomeOptionalNullFields.toString()))

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestDataWithSomeOptionalNullFields)
            .futureValue

          response.status shouldBe CREATED
          response.json   shouldBe toJson(
            createRecordResponseDataWithSomeOptionalNullFields.as[CreateOrUpdateRecordResponse]
          )

          verifyThatDownstreamApiWasCalled()
        }
        "with only required fields" in {
          stubForEis(CREATED, Some(createRecordRequiredResponseData.toString()))

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequiredRequestData)
            .futureValue

          response.status shouldBe CREATED
          response.json   shouldBe toJson(createRecordRequiredResponseData.as[CreateOrUpdateRecordResponse])

          verifyThatDownstreamApiWasCalled()
        }
      }
      "valid but the integration call fails with response:" - {
        "Forbidden" in {
          stubForEis(FORBIDDEN)

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestData)
            .futureValue

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

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe NOT_FOUND
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "NOT_FOUND",
            "message"       -> "Not Found"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Gateway" in {
          stubForEis(BAD_GATEWAY)

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe BAD_GATEWAY
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_GATEWAY",
            "message"       -> "Bad Gateway"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Service Unavailable" in {
          stubForEis(SERVICE_UNAVAILABLE)

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe SERVICE_UNAVAILABLE
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "SERVICE_UNAVAILABLE",
            "message"       -> "Service Unavailable"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error  with 201 errorCode" in {
          stubForEis(
            INTERNAL_SERVER_ERROR,
            Some(eisErrorResponse("201", "Internal Server Error"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "INVALID_OR_EMPTY_PAYLOAD",
            "message"       -> "Invalid Response Payload or Empty payload"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error  with 401 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("401", "Unauthorised")))

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNAUTHORIZED",
            "message"       -> "Unauthorized"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error  with 500 errorCode" in {
          stubForEis(
            INTERNAL_SERVER_ERROR,
            Some(eisErrorResponse("500", "Internal Server Error"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "INTERNAL_SERVER_ERROR",
            "message"       -> "Internal Server Error"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 404 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("404", "Not Found")))

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "NOT_FOUND",
            "message"       -> "Not Found"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 405 errorCode" in {
          stubForEis(
            INTERNAL_SERVER_ERROR,
            Some(eisErrorResponse("405", "Method Not Allowed"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestData)
            .futureValue

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

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_GATEWAY",
            "message"       -> "Bad Gateway"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 503 errorCode" in {
          stubForEis(
            INTERNAL_SERVER_ERROR,
            Some(eisErrorResponse("503", "Service Unavailable"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "SERVICE_UNAVAILABLE",
            "message"       -> "Service Unavailable"
          )

          verifyThatDownstreamApiWasCalled()
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
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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

          verifyThatDownstreamApiWasCalled()
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
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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

          verifyThatDownstreamApiWasCalled()
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
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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

          verifyThatDownstreamApiWasCalled()
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
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNEXPECTED_ERROR",
            "message"       -> s"Unable to parse fault detail for correlation Id: $correlationId"
          )

          verifyThatDownstreamApiWasCalled()
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
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNEXPECTED_ERROR",
            "message"       -> "Unexpected Error"
          )

          verifyThatDownstreamApiWasCalled()
        }
      }
      "invalid, specifically" - {
        "missing required header" in {
          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Missing mandatory header X-Client-ID"
          )

          verifyThatDownstreamApiWasNotCalled()
        }
        "missing required request field" in {
          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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

          verifyThatDownstreamApiWasNotCalled()
        }
        "category field is out of range" in {
          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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

          verifyThatDownstreamApiWasNotCalled()
        }
        "for optional fields" in {
          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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

          verifyThatDownstreamApiWasNotCalled()
        }
        "for optional assessment array fields" in {
          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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

          verifyThatDownstreamApiWasNotCalled()
        }
        "for mandatory fields actorId and comcode" in {
          val response = wsClient
            .url(url)
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
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

          verifyThatDownstreamApiWasNotCalled()
        }
      }
    }
  }

  private def stubForEis(httpStatus: Int, responseBody: Option[String] = None) = stubFor(
    post(urlEqualTo(s"$connectorPath"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(httpStatus)
          .withBody(responseBody.orNull)
      )
  )

  lazy val createRecordResponseData: JsValue =
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

  lazy val createEisRecordResponseDataWithOptionalNullFields: JsValue =
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

  lazy val createEisRecordResponseDataWithConditionOptionalNullFields: JsValue =
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

  lazy val createEisRecordResponseDataWithSomeOptionalNullFields: JsValue =
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
               |      "primaryCategory": 1
               |    }
               |  ]
               |}
               |""".stripMargin)

  lazy val createRecordResponseDataWithOptionalNullFields: JsValue =
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
               |  "assessments": []
               |}
               |""".stripMargin)

  lazy val createRecordResponseDataWithSomeOptionalNullFields: JsValue =
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
               "assessments": [
               |    {
               |      "primaryCategory": 1
               |    }
               |  ]
               |}
               |""".stripMargin)

  lazy val createRecordRequestData: String =
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

  lazy val createRecordRequestDataWithOptionalNullFields: String =
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

  lazy val createRecordRequestDataWithConditionOptionalNullFields: String =
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

  lazy val createRecordRequestDataWithSomeOptionalNullFields: String =
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

  lazy val createRecordRequiredRequestData: String =
    """
      |{
      |    "eori": "GB123456789012",
      |    "actorId": "GB098765432112",
      |    "traderRef": "BAN001001",
      |    "comcode": "10410100",
      |    "goodsDescription": "Organic bananas",
      |    "countryOfOrigin": "EC",
      |    "category": 1,
      |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z"
      |}
      |""".stripMargin

  lazy val createRecordRequiredResponseData: JsValue =
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
          |    "toReview": false,
          |    "reviewReason": "Commodity code change",
          |    "declarable": "SPIMM",
          |    "ukimsNumber": "XIUKIM47699357400020231115081800",
          |    "nirmsNumber": "RMS-GB-123456",
          |    "niphlNumber": "6 S12345",
          |    "createdDateTime": "2024-11-18T23:20:19Z",
          |    "updatedDateTime": "2024-11-18T23:20:19Z"
          |}
          |""".stripMargin)

  lazy val invalidRequestData: String =
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

  lazy val invalidCategoryRequestData: String =
    """
      |{
      |  "eori": "GB123456789012",
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

  lazy val invalidOptionalRequestData: String =
    """
      |{
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

  lazy val invalidCreateRecordRequestDataForAssessmentArray: JsValue = Json
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

  lazy val invalidActorIdAndComcodeRequestData: String =
    """
      |{
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
