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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, equalToJson, post, stubFor, urlEqualTo}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, CREATED, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json

import java.time.Instant

class RequestAccreditationIntegrationSpec extends BaseIntegrationWithConnectorSpec with BeforeAndAfterEach {

  val correlationId                  = "d677693e-9981-4ee3-8574-654981ebe606"
  val dateTime                       = "2021-12-17T09:30:47.456Z"
  val timestamp                      = "Fri, 17 Dec 2021 09:30:47 Z"
  override def connectorPath: String = "/tgp/createaccreditation/v1"
  override def connectorName: String = "eis"

  override def beforeEach: Unit = {
    super.beforeEach()
    when(uuidService.uuid).thenReturn("d677693e-9981-4ee3-8574-654981ebe606")
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }

  "attempting to create accreditation, when" - {
    "the request is" - {
      "valid, specifically" - {
        "with all request fields" in {
          stubForEis(CREATED, createAccreditationRequestData)

          val response = wsClient
            .url(fullUrl(s"/createaccreditation/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/json"))
            .post(createAccreditationRequestExternalApi)
            .futureValue

          response.status shouldBe CREATED

          verifyThatDownstreamApiWasCalled()
        }

        "with only required fields" in {
          stubForEis(CREATED, createAccreditationRequiredRequestData)
          val response = wsClient
            .url(fullUrl(s"/createaccreditation/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/json"))
            .post(createAccreditationRequestRequiredExternalApi)
            .futureValue

          response.status shouldBe CREATED

          verifyThatDownstreamApiWasCalled()
        }

      }
      "valid but the integration call fails with response:" - {
        "Forbidden" in {
          stubForEis(FORBIDDEN, createAccreditationRequestData)

          val response = wsClient
            .url(fullUrl(s"/createaccreditation/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/json"))
            .post(createAccreditationRequestData)
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
          stubForEis(NOT_FOUND, createAccreditationRequestData)

          val response = wsClient
            .url(fullUrl(s"/createaccreditation/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/json"))
            .post(createAccreditationRequestData)
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
          stubForEis(BAD_GATEWAY, createAccreditationRequestData)

          val response = wsClient
            .url(fullUrl(s"/createaccreditation/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/json"))
            .post(createAccreditationRequestData)
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
          stubForEis(SERVICE_UNAVAILABLE, createAccreditationRequestData)

          val response = wsClient
            .url(fullUrl(s"/createaccreditation/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/json"))
            .post(createAccreditationRequestData)
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
            createAccreditationRequestData,
            Some(eisErrorResponse("201", "Internal Server Error"))
          )

          val response = wsClient
            .url(fullUrl(s"/createaccreditation/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/json"))
            .post(createAccreditationRequestData)
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
          stubForEis(
            INTERNAL_SERVER_ERROR,
            createAccreditationRequestData,
            Some(eisErrorResponse("401", "Unauthorised"))
          )

          val response = wsClient
            .url(fullUrl(s"/createaccreditation/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/json"))
            .post(createAccreditationRequestData)
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
            createAccreditationRequestData,
            Some(eisErrorResponse("500", "Internal Server Error"))
          )

          val response = wsClient
            .url(fullUrl(s"/createaccreditation/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/json"))
            .post(createAccreditationRequestData)
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
          stubForEis(INTERNAL_SERVER_ERROR, createAccreditationRequestData, Some(eisErrorResponse("404", "Not Found")))

          val response = wsClient
            .url(fullUrl(s"/createaccreditation/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/json"))
            .post(createAccreditationRequestData)
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
            createAccreditationRequestData,
            Some(eisErrorResponse("405", "Method Not Allowed"))
          )

          val response = wsClient
            .url(fullUrl(s"/createaccreditation/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/json"))
            .post(createAccreditationRequestData)
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
          stubForEis(
            INTERNAL_SERVER_ERROR,
            createAccreditationRequestData,
            Some(eisErrorResponse("502", "Bad Gateway"))
          )

          val response = wsClient
            .url(fullUrl(s"/createaccreditation/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/json"))
            .post(createAccreditationRequestData)
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
            createAccreditationRequestData,
            Some(eisErrorResponse("503", "Service Unavailable"))
          )

          val response = wsClient
            .url(fullUrl(s"/createaccreditation/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/json"))
            .post(createAccreditationRequestData)
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
            createAccreditationRequestData,
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
            .url(fullUrl(s"/createaccreditation/"))
            .withHttpHeaders(("Content-Type", "application/json"), ("Accept", "application/json"))
            .post(createAccreditationRequestData)
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
      }
    }
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

  private def stubForEis(httpStatus: Int, requestBody: String, responseBody: Option[String] = None) = stubFor(
    post(urlEqualTo(s"$connectorPath"))
      .withRequestBody(equalToJson(requestBody))
      .withHeader("Content-Type", equalTo("application/json"))
      .withHeader("X-Forwarded-Host", equalTo("MDTP"))
      .withHeader("X-Correlation-ID", equalTo(correlationId))
      .withHeader("Date", equalTo(timestamp))
      .withHeader("Accept", equalTo("application/json"))
      .withHeader("Authorization", equalTo("bearerToken"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(httpStatus)
          .withBody(responseBody.orNull)
      )
  )

  lazy val createAccreditationRequiredRequestData: String =
    s"""
      |{
      |   "accreditationRequest":{
      |      "requestCommon":{
      |         "receiptDate":"$timestamp"
      |      },
      |      "requestDetail":{
      |         "traderDetails":{
      |            "traderEORI":"GB123456789001",
      |            "requestorName":"Mr.Phil Edwards",
      |            "requestorEmail":"Phil.Edwards@gmail.com",
      |            "ukimsAuthorisation":"UKIMSAUTH1234567",
      |            "goodsItems":[
      |               {
      |                  "publicRecordID":"GB1234567890011111111111111111111111",
      |                  "traderReference":"SKU123456",
      |                  "goodsDescription":"Strawberries",
      |                  "commodityCode":"0810100000"
      |               }
      |            ]
      |         }
      |      }
      |   }
      |}
      |""".stripMargin

  lazy val createAccreditationRequestData: String =
    s"""
      |{
      |   "accreditationRequest":{
      |      "requestCommon":{
      |         "clientID":"MDTP",
      |         "receiptDate":"$timestamp",
      |         "boxID":"String"
      |      },
      |      "requestDetail":{
      |         "traderDetails":{
      |            "traderEORI":"GB123456789001",
      |            "requestorName":"Mr.Phil Edwards",
      |            "requestorEORI":"XI123456789001",
      |            "requestorEmail":"Phil.Edwards@gmail.com",
      |            "ukimsAuthorisation":"UKIMSAUTH1234567",
      |            "goodsItems":[
      |               {
      |                  "publicRecordID":"GB1234567890011111111111111111111111",
      |                  "traderReference":"SKU123456",
      |                  "goodsDescription":"Strawberries",
      |                  "countryOfOrigin":"GB",
      |                  "supplementaryUnit":123456,
      |                  "category":1,
      |                  "measurementUnitDescription":"Kilogram of nitrogen",
      |                  "commodityCode":"0810100000"
      |               }
      |            ]
      |         }
      |      }
      |   }
      |}
      |""".stripMargin

  lazy val createAccreditationRequestRequiredExternalApi: String =
    """
      |{
      |   "traderEORI":"GB123456789001",
      |   "requestorName":"Mr.Phil Edwards",
      |   "requestorEmail":"Phil.Edwards@gmail.com",
      |   "ukimsAuthorisation":"UKIMSAUTH1234567",
      |   "goodsItems":[
      |      {
      |         "publicRecordID":"GB1234567890011111111111111111111111",
      |         "traderReference":"SKU123456",
      |         "goodsDescription":"Strawberries",
      |         "commodityCode":"0810100000"
      |      }
      |   ]
      |}
      |""".stripMargin

  lazy val createAccreditationRequestExternalApi: String =
    """
      |{
      |   "traderEORI":"GB123456789001",
      |   "requestorName":"Mr.Phil Edwards",
      |   "requestorEORI":"XI123456789001",
      |   "requestorEmail":"Phil.Edwards@gmail.com",
      |   "ukimsAuthorisation":"UKIMSAUTH1234567",
      |   "goodsItems":[
      |      {
      |         "publicRecordID":"GB1234567890011111111111111111111111",
      |         "traderReference":"SKU123456",
      |         "goodsDescription":"Strawberries",
      |         "countryOfOrigin":"GB",
      |         "supplementaryUnit":123456,
      |         "category":1,
      |         "measurementUnitDescription":"Kilogram of nitrogen",
      |         "commodityCode":"0810100000"
      |      }
      |   ]
      |}
      |""".stripMargin

}
