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

class UpdateRecordIntegrationSpec extends BaseIntegrationWithConnectorSpec with BeforeAndAfterEach {

  val correlationId                  = "d677693e-9981-4ee3-8574-654981ebe606"
  val dateTime                       = "2021-12-17T09:30:47.456Z"
  val timestamp                      = "Fri, 17 Dec 2021 09:30:47 Z"
  override def connectorPath: String = "/tgp/updaterecord/v1"
  override def connectorName: String = "eis"

  override def beforeEach: Unit = {
    super.beforeEach()
    when(uuidService.uuid).thenReturn("d677693e-9981-4ee3-8574-654981ebe606")
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }

  "attempting to update a record, when" - {
    "the request is" - {
      "valid, specifically" - {
        "with all request fields" in {
          stubForEis(OK, updateRecordRequestData, Some(updateRecordResponseData.toString()))

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
            .futureValue

          response.status shouldBe OK
          response.json   shouldBe toJson(updateRecordResponseData.as[CreateOrUpdateRecordResponse])

          verifyThatDownstreamApiWasCalled()
        }
        "with only required fields" in {
          stubForEis(OK, updateRecordRequiredRequestData, Some(updateRecordRequiredResponseData.toString()))

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequiredRequestData)
            .futureValue

          response.status shouldBe OK
          response.json   shouldBe toJson(updateRecordRequiredResponseData.as[CreateOrUpdateRecordResponse])

          verifyThatDownstreamApiWasCalled()
        }
      }
      "valid but the integration call fails with response:" - {
        "Forbidden" in {
          stubForEis(FORBIDDEN, updateRecordRequestData)

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
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
          stubForEis(NOT_FOUND, updateRecordRequestData)

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
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
          stubForEis(BAD_GATEWAY, updateRecordRequestData)

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
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
          stubForEis(SERVICE_UNAVAILABLE, updateRecordRequestData)

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
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
            updateRecordRequestData,
            Some(eisErrorResponse("201", "Internal Server Error"))
          )

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
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
          stubForEis(INTERNAL_SERVER_ERROR, updateRecordRequestData, Some(eisErrorResponse("401", "Unauthorised")))

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
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
            updateRecordRequestData,
            Some(eisErrorResponse("500", "Internal Server Error"))
          )

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
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
          stubForEis(INTERNAL_SERVER_ERROR, updateRecordRequestData, Some(eisErrorResponse("404", "Not Found")))

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
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
            updateRecordRequestData,
            Some(eisErrorResponse("405", "Method Not Allowed"))
          )

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
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
          stubForEis(INTERNAL_SERVER_ERROR, updateRecordRequestData, Some(eisErrorResponse("502", "Bad Gateway")))

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
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
            updateRecordRequestData,
            Some(eisErrorResponse("503", "Service Unavailable"))
          )

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
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
            updateRecordRequestData,
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
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"    -> "INVALID_REQUEST_PARAMETER",
                "message" -> "Mandatory field eori was missing from body or is in the wrong format",
                "errorNumber" -> 6
              )
            )
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request with more than one error details" in {
          stubForEis(
            BAD_REQUEST,
            updateRecordRequestData,
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
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"    -> "INVALID_REQUEST_PARAMETER",
                "message" -> "The recordId has been provided in the wrong format",
                "errorNumber" -> 25
              ),
              Json.obj(
                "code"    -> "INVALID_REQUEST_PARAMETER",
                "message" -> "The requested recordId to update doesn’t exist",
                "errorNumber" -> 26
              )
            )
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request with unexpected error" in {
          stubForEis(
            BAD_REQUEST,
            updateRecordRequestData,
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
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code" -> "UNEXPECTED_ERROR",
                "message" -> "Unrecognised error number",
                "errorNumber" -> 40
              )
            )
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request with unable to parse the detail" in {
          stubForEis(
            BAD_REQUEST,
            updateRecordRequestData,
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
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
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
            updateRecordRequestData,
            Some(s"""
                 | {
                 |    "invalid": "error"
                 |  }
                 |""".stripMargin)
          )

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(updateRecordRequestData)
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
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"))
            .put(updateRecordRequestData)
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
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
            .put(invalidRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"    -> "INVALID_REQUEST_PARAMETER",
                "message" -> "Mandatory field eori was missing from body or is in the wrong format",
                "errorNumber" -> 6
              ),
              Json.obj(
                "code"    -> "INVALID_REQUEST_PARAMETER",
                "message" -> "The recordId has been provided in the wrong format",
                "errorNumber" -> 26
              )
            )
          )

          verifyThatDownstreamApiWasNotCalled()
        }
      }
    }
  }

  private def stubForEis(httpStatus: Int, requestBody: String, responseBody: Option[String] = None) = stubFor(
    put(urlEqualTo(s"$connectorPath"))
      .withRequestBody(equalToJson(requestBody))
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
          .withBody(responseBody.orNull)
      )
  )

  lazy val updateRecordResponseData: JsValue =
    Json
      .parse("""
        |{
        |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
        |  "eori": "GB123456789012",
        |  "actorId": "GB098765432112",
        |  "traderRef": "BAN001001",
        |  "comcode": "104101000",
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

  lazy val updateRecordRequestData: String =
    """
        |{
        |    "eori": "GB123456789001",
        |    "actorId": "GB098765432112",
        |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        |    "traderRef": "BAN001001",
        |    "comcode": "104101000",
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

  lazy val updateRecordRequiredRequestData: String =
    """
      |{
      |    "eori": "GB123456789012",
      |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
      |    "actorId": "GB098765432112",
      |    "traderRef": "BAN001001",
      |    "comcode": "104101000",
      |    "goodsDescription": "Organic bananas",
      |    "countryOfOrigin": "EC",
      |    "category": 1,
      |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z"
      |}
      |""".stripMargin

  lazy val updateRecordRequiredResponseData: JsValue =
    Json
      .parse("""
          |{
          |    "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
          |    "eori": "GB123456789012",
          |    "actorId": "GB098765432112",
          |    "traderRef": "BAN001001",
          |    "comcode": "104101000",
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
      |    "actorId": "GB098765432112",
      |    "traderRef": "BAN001001",
      |    "comcode": "104101000",
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
