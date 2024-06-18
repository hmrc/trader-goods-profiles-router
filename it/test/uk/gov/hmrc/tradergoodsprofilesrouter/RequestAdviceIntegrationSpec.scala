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
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport

import java.time.Instant

class RequestAdviceIntegrationSpec
    extends BaseIntegrationWithConnectorSpec
    with GetRecordsDataSupport
    with BeforeAndAfterEach {

  private val correlationId          = "d677693e-9981-4ee3-8574-654981ebe606"
  private val timestamp              = "Fri, 17 Dec 2021 09:30:47 GMT"
  private val eori                   = "GB123456789001"
  private val recordId               = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  private val url                    = fullUrl(s"/traders/$eori/records/$recordId/advice")
  override def connectorPath: String = "/tgp/createaccreditation/v1"
  def connectorPathGetRecord: String = "/tgp/getrecords/v1"
  override def connectorName: String = "eis"

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(uuidService.uuid).thenReturn("d677693e-9981-4ee3-8574-654981ebe606")
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }

  "attempting to create accreditation, when" - {
    "the request is" - {
      "valid, specifically" - {
        "with all request fields" in {
          stubForEisFetchRecords(OK, getEisRecordsResponseData.toString())
          stubForEis(CREATED, createAccreditationRequestData)

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(
                ("Content-Type", "application/json"),
                ("Accept", "application/json"),
                ("X-Client-ID", "tss")
              )
              .post(requestAdviceData)
          )

          response.status shouldBe CREATED

          verifyThatMultipleDownstreamApiWasCalled()
        }

      }
      "valid but the integration call fails with response:" - {
        "Forbidden" in {
          stubForEisFetchRecords(OK, getEisRecordsResponseData.toString())
          stubForEis(FORBIDDEN, createAccreditationRequestData)

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/json"),
              ("X-Client-ID", "tss")
            )
            .post(requestAdviceData)
            .futureValue

          response.status shouldBe FORBIDDEN
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "FORBIDDEN",
            "message"       -> "Forbidden"
          )

          verifyThatMultipleDownstreamApiWasCalled()
        }

        "Not Found" in {
          stubForEisFetchRecords(OK, getEisRecordsResponseData.toString())
          stubForEis(NOT_FOUND, createAccreditationRequestData)

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/json"),
              ("X-Client-ID", "tss")
            )
            .post(requestAdviceData)
            .futureValue

          response.status shouldBe NOT_FOUND
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "NOT_FOUND",
            "message"       -> "Not Found"
          )

          verifyThatMultipleDownstreamApiWasCalled()
        }
        "Bad Gateway" in {
          stubForEisFetchRecords(OK, getEisRecordsResponseData.toString())
          stubForEis(BAD_GATEWAY, createAccreditationRequestData)

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/json"),
              ("X-Client-ID", "tss")
            )
            .post(requestAdviceData)
            .futureValue

          response.status shouldBe BAD_GATEWAY
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_GATEWAY",
            "message"       -> "Bad Gateway"
          )

          verifyThatMultipleDownstreamApiWasCalled()
        }

        "Service Unavailable for fetch records" in {
          val errorResponseJson = Json.obj("error" -> "error")
          stubForEisFetchRecords(SERVICE_UNAVAILABLE, errorResponseJson.toString())
          stubForEis(SERVICE_UNAVAILABLE, createAccreditationRequestData)

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/json"),
              ("X-Client-ID", "tss")
            )
            .post(requestAdviceData)
            .futureValue

          response.status shouldBe SERVICE_UNAVAILABLE
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "SERVICE_UNAVAILABLE",
            "message"       -> "Service Unavailable"
          )

          verifyThatDownstreamApiWasNotCalled()
        }
        "Service Unavailable" in {
          stubForEisFetchRecords(OK, getEisRecordsResponseData.toString())
          stubForEis(SERVICE_UNAVAILABLE, createAccreditationRequestData)

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/json"),
              ("X-Client-ID", "tss")
            )
            .post(requestAdviceData)
            .futureValue

          response.status shouldBe SERVICE_UNAVAILABLE
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "SERVICE_UNAVAILABLE",
            "message"       -> "Service Unavailable"
          )

          verifyThatMultipleDownstreamApiWasCalled()
        }

        "Internal Server Error  with 201 errorCode" in {
          stubForEisFetchRecords(OK, getEisRecordsResponseData.toString())
          stubForEis(
            INTERNAL_SERVER_ERROR,
            createAccreditationRequestData,
            Some(eisErrorResponse("201", "Internal Server Error"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/json"),
              ("X-Client-ID", "tss")
            )
            .post(requestAdviceData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "INVALID_OR_EMPTY_PAYLOAD",
            "message"       -> "Invalid Response Payload or Empty payload"
          )

          verifyThatMultipleDownstreamApiWasCalled()
        }
        "Internal Server Error  with 401 errorCode" in {
          stubForEisFetchRecords(OK, getEisRecordsResponseData.toString())
          stubForEis(
            INTERNAL_SERVER_ERROR,
            createAccreditationRequestData,
            Some(eisErrorResponse("401", "Unauthorised"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/json"),
              ("X-Client-ID", "tss")
            )
            .post(requestAdviceData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNAUTHORIZED",
            "message"       -> "Unauthorized"
          )

          verifyThatMultipleDownstreamApiWasCalled()
        }
        "Internal Server Error  with 500 errorCode" in {
          stubForEisFetchRecords(OK, getEisRecordsResponseData.toString())
          stubForEis(
            INTERNAL_SERVER_ERROR,
            createAccreditationRequestData,
            Some(eisErrorResponse("500", "Internal Server Error"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/json"),
              ("X-Client-ID", "tss")
            )
            .post(requestAdviceData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "INTERNAL_SERVER_ERROR",
            "message"       -> "Internal Server Error"
          )

          verifyThatMultipleDownstreamApiWasCalled()
        }
        "Internal Server Error with 404 errorCode" in {
          stubForEisFetchRecords(OK, getEisRecordsResponseData.toString())
          stubForEis(INTERNAL_SERVER_ERROR, createAccreditationRequestData, Some(eisErrorResponse("404", "Not Found")))

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/json"),
              ("X-Client-ID", "tss")
            )
            .post(requestAdviceData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "NOT_FOUND",
            "message"       -> "Not Found"
          )

          verifyThatMultipleDownstreamApiWasCalled()
        }
        "Internal Server Error with 405 errorCode" in {
          stubForEisFetchRecords(OK, getEisRecordsResponseData.toString())
          stubForEis(
            INTERNAL_SERVER_ERROR,
            createAccreditationRequestData,
            Some(eisErrorResponse("405", "Method Not Allowed"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/json"),
              ("X-Client-ID", "tss")
            )
            .post(requestAdviceData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "METHOD_NOT_ALLOWED",
            "message"       -> "Method Not Allowed"
          )

          verifyThatMultipleDownstreamApiWasCalled()
        }
        "Internal Server Error with 502 errorCode" in {
          stubForEisFetchRecords(OK, getEisRecordsResponseData.toString())
          stubForEis(
            INTERNAL_SERVER_ERROR,
            createAccreditationRequestData,
            Some(eisErrorResponse("502", "Bad Gateway"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/json"),
              ("X-Client-ID", "tss")
            )
            .post(requestAdviceData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_GATEWAY",
            "message"       -> "Bad Gateway"
          )

          verifyThatMultipleDownstreamApiWasCalled()
        }
        "Internal Server Error with 503 errorCode" in {
          stubForEisFetchRecords(OK, getEisRecordsResponseData.toString())
          stubForEis(
            INTERNAL_SERVER_ERROR,
            createAccreditationRequestData,
            Some(eisErrorResponse("503", "Service Unavailable"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              ("Content-Type", "application/json"),
              ("Accept", "application/json"),
              ("X-Client-ID", "tss")
            )
            .post(requestAdviceData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "SERVICE_UNAVAILABLE",
            "message"       -> "Service Unavailable"
          )

          verifyThatMultipleDownstreamApiWasCalled()
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
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(httpStatus)
          .withBody(responseBody.orNull)
      )
  )

  private def stubForEisFetchRecords(httpStatus: Int, body: String) = stubFor(
    get(urlEqualTo(s"$connectorPathGetRecord/$eori/$recordId"))
      .willReturn(
        aResponse()
          .withStatus(httpStatus)
          .withBody(body)
      )
  )

  private def createAccreditationRequestData: String =
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
      |            "requestorEORI":"GB1234567890",
      |            "requestorEmail":"Phil.Edwards@gmail.com",
      |            "ukimsAuthorisation":"XIUKIM47699357400020231115081800",
      |            "goodsItems":[
      |               {
      |                  "publicRecordID":"8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
      |                  "traderReference":"BAN001001",
      |                  "goodsDescription":"Organic bananas",
      |                  "countryOfOrigin":"EC",
      |                  "supplementaryUnit":500,
      |                  "category":3,
      |                  "measurementUnitDescription":"square meters(m^2)",
      |                  "commodityCode":"10410100"
      |               }
      |            ]
      |         }
      |      }
      |   }
      |}
      |""".stripMargin

  private def requestAdviceData: String =
    s"""
             |{
             |    "requestorName": "Mr.Phil Edwards",
             |    "requestorEmail": "Phil.Edwards@gmail.com"
             |}
             |""".stripMargin

}
