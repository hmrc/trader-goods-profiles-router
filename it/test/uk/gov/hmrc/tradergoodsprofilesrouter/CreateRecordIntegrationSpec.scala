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
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateRecordResponse

import java.time.Instant

class CreateRecordIntegrationSpec extends BaseIntegrationWithConnectorSpec with BeforeAndAfterEach {

  val correlationId                  = "d677693e-9981-4ee3-8574-654981ebe606"
  val dateTime                       = "2021-12-17T09:30:47.456Z"
  val timestamp                      = "Fri, 17 Dec 2021 09:30:47 Z"
  override def connectorPath: String = "/tgp/createrecord/v1"
  override def connectorName: String = "eis"

  override def beforeEach: Unit = {
    super.beforeEach()
    when(uuidService.uuid).thenReturn("d677693e-9981-4ee3-8574-654981ebe606")
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }

  "attempting to get records, when" - {
    "the request is" - {
      "valid, specifically" - {
        "with all request fields" in {
          stubForEis(CREATED, createRecordRequestData, Some(createRecordResponseData.toString()))

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe CREATED
          response.json   shouldBe toJson(createRecordResponseData.as[CreateRecordResponse])

          verifyThatDownstreamApiWasCalled()
        }
        "with only required fields" in {
          stubForEis(CREATED, createRecordRequiredRequestData, Some(createRecordRequiredResponseData.toString()))

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
            .post(createRecordRequiredRequestData)
            .futureValue

          response.status shouldBe CREATED
          response.json   shouldBe toJson(createRecordRequiredResponseData.as[CreateRecordResponse])

          verifyThatDownstreamApiWasCalled()
        }
      }
      "valid but the integration call fails with response:" - {
        "Forbidden" in {
          stubForEis(FORBIDDEN, createRecordRequestData)

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
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
          stubForEis(NOT_FOUND, createRecordRequestData)

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
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
          stubForEis(BAD_GATEWAY, createRecordRequestData)

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
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
          stubForEis(SERVICE_UNAVAILABLE, createRecordRequestData)

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
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
            createRecordRequestData,
            Some(eisErrorResponse("201", "Internal Server Error"))
          )

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
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
          stubForEis(INTERNAL_SERVER_ERROR, createRecordRequestData, Some(eisErrorResponse("401", "Unauthorised")))

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
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
            createRecordRequestData,
            Some(eisErrorResponse("500", "Internal Server Error"))
          )

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
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
          stubForEis(INTERNAL_SERVER_ERROR, createRecordRequestData, Some(eisErrorResponse("404", "Not Found")))

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
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
            createRecordRequestData,
            Some(eisErrorResponse("405", "Method Not Allowed"))
          )

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
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
          stubForEis(INTERNAL_SERVER_ERROR, createRecordRequestData, Some(eisErrorResponse("502", "Bad Gateway")))

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
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
            createRecordRequestData,
            Some(eisErrorResponse("503", "Service Unavailable"))
          )

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
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
            createRecordRequestData,
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
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
            .post(createRecordRequestData)
            .futureValue

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
        "Bad Request with more than one error details" in {
          stubForEis(
            BAD_REQUEST,
            createRecordRequestData,
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
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Bad Request",
            "errors"        -> Json.arr(
              Json.obj(
                "code"    -> "025",
                "message" -> "The recordId has been provided in the wrong format"
              ),
              Json.obj(
                "code"    -> "026",
                "message" -> "The requested recordId to update doesn’t exist"
              )
            )
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request with unexpected error" in {
          stubForEis(
            BAD_REQUEST,
            createRecordRequestData,
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
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
            .post(createRecordRequestData)
            .futureValue

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
          stubForEis(
            BAD_REQUEST,
            createRecordRequestData,
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
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "statusCode" -> 500,
            "message"    -> s"Unable to parse fault detail for correlation Id: $correlationId"
          )

          verifyThatDownstreamApiWasCalled()
        }
        "Bad Request with invalid json" in {
          stubForEis(
            BAD_REQUEST,
            createRecordRequestData,
            Some(s"""
                 | {
                 |    "invalid": "error"
                 |  }
                 |""".stripMargin)
          )

          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
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
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"))
            .post(createRecordRequestData)
            .futureValue

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_REQUEST",
            "message"       -> "Missing mandatory header X-Client-Id"
          )

          verifyThatDownstreamApiWasNotCalled()
        }
        "missing required request field" in {
          val response = wsClient
            .url(fullUrl(s"/records/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
            .post(invalidRequestData)
            .futureValue

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

          verifyThatDownstreamApiWasNotCalled()
        }
      }
    }
  }

  private def stubForEis(httpStatus: Int, requestBody: String, responseBody: Option[String] = None) = stubFor(
    post(urlEqualTo(s"$connectorPath"))
      .withRequestBody(equalToJson(requestBody))
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
        |  "createdDateTime": "2024-11-18T23->20->19Z",
        |  "updatedDateTime": "2024-11-18T23->20->19Z",
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

  lazy val createRecordRequestData: String =
    """
        |{
        |    "eori": "GB123456789012",
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

  lazy val createRecordRequiredRequestData: String =
    """
      |{
      |    "eori": "GB123456789012",
      |    "actorId": "GB098765432112",
      |    "traderRef": "BAN001001",
      |    "comcode": "104101000",
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
          |    "createdDateTime": "2024-11-18T23->20->19Z",
          |    "updatedDateTime": "2024-11-18T23->20->19Z"
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