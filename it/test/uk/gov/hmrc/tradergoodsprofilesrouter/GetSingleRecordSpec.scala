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

import java.time.Instant

class GetSingleRecordSpec extends BaseIntegrationWithConnectorSpec with BeforeAndAfterEach {

  val eori                           = "GB123456789001"
  val recordId                       = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  override def connectorPath: String = s"/tgp/getrecords/v1"
  override def connectorName: String = "eis"

  override def beforeEach: Unit = {
    super.beforeEach()
    when(uuidService.uuid).thenReturn("d677693e-9981-4ee3-8574-654981ebe606")
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }

  "attempting to get records, when" - {
    "the request is" - {
      "valid" in {

        stubFor(
          get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
            .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
            .withHeader("X-Forwarded-Host", equalTo("MDTP"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Authorization", equalTo("bearerToken"))
            .withHeader("X-Client-Id", equalTo("tss"))
            .willReturn(
              aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(OK)
                .withBody(s"""
                       |{
                       |    "goodsItemRecords": [
                       |        {
                       |            "eori": "GB1234567890",
                       |            "actorId": "GB1234567890",
                       |            "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
                       |            "traderRef": "BAN001001",
                       |            "comcode": "104101000",
                       |            "accreditationRequest": "Not requested",
                       |            "goodsDescription": "Organic bananas",
                       |            "countryOfOrigin": "EC",
                       |            "category": 3,
                       |            "assessments": [
                       |                {
                       |                    "assessmentId": "abc123",
                       |                    "primaryCategory": "1",
                       |                    "condition": {
                       |                        "type": "abc123",
                       |                        "conditionId": "Y923",
                       |                        "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
                       |                        "conditionTraderText": "Excluded product"
                       |                    }
                       |                }
                       |            ],
                       |            "supplementaryUnit": 500,
                       |            "measurementUnit": "square meters(m^2)",
                       |            "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
                       |            "comcodeEffectiveToDate": "",
                       |            "version": 1,
                       |            "active": true,
                       |            "toReview": false,
                       |            "reviewReason": null,
                       |            "declarable": "IMMI declarable",
                       |            "ukimsNumber": "XIUKIM47699357400020231115081800",
                       |            "nirmsNumber": "RMS-GB-123456",
                       |            "niphlNumber": "6 S12345",
                       |            "locked": false,
                       |            "srcSystemName": "CDAP",
                       |            "createdDateTime": "2024-11-18T23:20:19Z",
                       |            "updatedDateTime": "2024-11-18T23:20:19Z"
                       |        }
                       |    ],
                       |    "pagination": {
                       |        "totalRecords": 1,
                       |        "currentPage": 0,
                       |        "totalPages": 1,
                       |        "nextPage": null,
                       |        "prevPage": null
                       |    }
                       |}
                       |""".stripMargin)
            )
        )

        val response = wsClient
          .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
          .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
          .get()
          .futureValue

        assertAsExpected(
          response = response,
          status = OK,
          jsonBodyOpt = Some(
            """
                |{
                |    "eori": "GB1234567890",
                |    "actorId": "GB1234567890",
                |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
                |    "traderRef": "BAN001001",
                |    "comcode": "104101000",
                |    "accreditationRequest": "Not requested",
                |    "goodsDescription": "Organic bananas",
                |    "countryOfOrigin": "EC",
                |    "category": 3,
                |    "assessments": null,
                |    "supplementaryUnit": 500,
                |    "measurementUnit": "square meters(m^2)",
                |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
                |    "comcodeEffectiveToDate": "",
                |    "version": 1,
                |    "active": true,
                |    "toReview": false,
                |    "reviewReason": null,
                |    "declarable": "IMMI declarable",
                |    "ukimsNumber": "XIUKIM47699357400020231115081800",
                |    "nirmsNumber": "RMS-GB-123456",
                |    "niphlNumber": "6 S12345",
                |    "locked": false,
                |    "srcSystemName": "CDAP",
                |    "createdDateTime": "2024-11-18T23:20:19Z",
                |    "updatedDateTime": "2024-11-18T23:20:19Z"
                |}
                |""".stripMargin
          )
        )
        verifyThatDownstreamApiWasCalled()
      }
      "valid but the integration call fails with response:" - {
        "Forbidden" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(FORBIDDEN)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = FORBIDDEN,
            jsonBodyOpt = Some(
              """
                |{
                |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                |    "code": "FORBIDDEN",
                |    "message": "Forbidden"
                |}
                |""".stripMargin
            )
          )
          verifyThatDownstreamApiWasCalled()
        }
        "Not Found" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(NOT_FOUND)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = NOT_FOUND,
            jsonBodyOpt = Some(
              """
                |{
                |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                |    "code": "NOT_FOUND",
                |    "message": "Not Found"
                |}
                |""".stripMargin
            )
          )
          verifyThatDownstreamApiWasCalled()
        }
        "Method Not Allowed" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(METHOD_NOT_ALLOWED)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = METHOD_NOT_ALLOWED,
            jsonBodyOpt = Some(
              """
                |{
                |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                |    "code": "METHOD_NOT_ALLOWED",
                |    "message": "Method Not Allowed"
                |}
                |""".stripMargin
            )
          )
          verifyThatDownstreamApiWasCalled()
        }
        "Unexpected Error" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(SERVICE_UNAVAILABLE)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = INTERNAL_SERVER_ERROR,
            jsonBodyOpt = Some(
              """
                |{
                |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                |    "code": "UNEXPECTED_ERROR",
                |    "message": "Unexpected Error"
                |}
                |""".stripMargin
            )
          )
          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(INTERNAL_SERVER_ERROR)
                  .withBody(s"""
                               | {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "500",
                               |    "errorMessage": "Internal Server Error",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": null
                               |    }
                               |  }
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
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
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(INTERNAL_SERVER_ERROR)
                  .withBody(s"""
                               | {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "200",
                               |    "errorMessage": "Internal Server Error",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": null
                               |    }
                               |  }
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = INTERNAL_SERVER_ERROR,
            jsonBodyOpt = Some(
              """
                |{
                |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                |    "code": "INVALID_OR_EMPTY_PAYLOAD",
                |    "message": "Invalid Response Payload or Empty payload"
                |}
                |""".stripMargin
            )
          )
          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 400 errorCode" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(INTERNAL_SERVER_ERROR)
                  .withBody(s"""
                               | {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "400",
                               |    "errorMessage": "Internal Error Response",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": null
                               |    }
                               |  }
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = INTERNAL_SERVER_ERROR,
            jsonBodyOpt = Some(
              """
                |{
                |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                |    "code": "INTERNAL_ERROR_RESPONSE",
                |    "message": "Internal Error Response"
                |}
                |""".stripMargin
            )
          )
          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 401 errorCode" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(INTERNAL_SERVER_ERROR)
                  .withBody(s"""
                               | {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "401",
                               |    "errorMessage": "Unauthorised",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": null
                               |    }
                               |  }
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = INTERNAL_SERVER_ERROR,
            jsonBodyOpt = Some(
              """
                |{
                |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                |    "code": "UNAUTHORIZED",
                |    "message": "Unauthorized"
                |}
                |""".stripMargin
            )
          )
          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 404 errorCode" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(INTERNAL_SERVER_ERROR)
                  .withBody(s"""
                               | {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "404",
                               |    "errorMessage": "Not Found",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": null
                               |    }
                               |  }
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = INTERNAL_SERVER_ERROR,
            jsonBodyOpt = Some(
              """
                |{
                |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                |    "code": "NOT_FOUND",
                |    "message": "Not Found"
                |}
                |""".stripMargin
            )
          )
          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 405 errorCode" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(INTERNAL_SERVER_ERROR)
                  .withBody(s"""
                               | {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "405",
                               |    "errorMessage": "Method Not Allowed",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": null
                               |    }
                               |  }
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = INTERNAL_SERVER_ERROR,
            jsonBodyOpt = Some(
              """
                |{
                |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                |    "code": "METHOD_NOT_ALLOWED",
                |    "message": "Method Not Allowed"
                |}
                |""".stripMargin
            )
          )
          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 502 errorCode" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(INTERNAL_SERVER_ERROR)
                  .withBody(s"""
                               | {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "502",
                               |    "errorMessage": "Bad Gateway",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": null
                               |    }
                               |  }
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = INTERNAL_SERVER_ERROR,
            jsonBodyOpt = Some(
              """
                |{
                |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                |    "code": "BAD_GATEWAY",
                |    "message": "Bad Gateway"
                |}
                |""".stripMargin
            )
          )
          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with 503 errorCode" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(INTERNAL_SERVER_ERROR)
                  .withBody(s"""
                               | {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "503",
                               |    "errorMessage": "Service Unavailable",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": null
                               |    }
                               |  }
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = INTERNAL_SERVER_ERROR,
            jsonBodyOpt = Some(
              """
                |{
                |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                |    "code": "SERVICE_UNAVAILABLE",
                |    "message": "Service Unavailable"
                |}
                |""".stripMargin
            )
          )
          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with unknown errorCode" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(INTERNAL_SERVER_ERROR)
                  .withBody(s"""
                               | {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "501",
                               |    "errorMessage": "Not Implemented",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": null
                               |    }
                               |  }
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = INTERNAL_SERVER_ERROR,
            jsonBodyOpt = Some(
              """
                |{
                |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                |    "code": "UNKNOWN",
                |    "message": "Unknown Error"
                |}
                |""".stripMargin
            )
          )
          verifyThatDownstreamApiWasCalled()
        }
        "Internal Server Error with unexpected error" in {
          stubFor(
            get(urlEqualTo(s"$connectorPath/$eori/$recordId"))
              .withHeader("Content-Type", equalTo("application/json"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 Z"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("bearerToken"))
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(INTERNAL_SERVER_ERROR)
                  .withBody(s"""
                       | {
                       |    "invalid": "error"
                       |  }
                       |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = INTERNAL_SERVER_ERROR,
            jsonBodyOpt = Some(
              """
                |{
                |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                |    "code": "UNEXPECTED_ERROR",
                |    "message": "Unexpected Error"
                |}
                |""".stripMargin
            )
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
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(BAD_REQUEST)
                  .withBody(s"""
                               | {
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
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/null/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = BAD_REQUEST,
            jsonBodyOpt = Some(
              """
                |{
                |  "correlationId" : "d677693e-9981-4ee3-8574-654981ebe606",
                |  "code" : "BAD_REQUEST",
                |  "message" : "Bad Request",
                |  "errors" : [ {
                |    "code" : "INVALID_REQUEST_PARAMETER",
                |    "message" : "006 - Missing or invalid mandatory request parameter EORI"
                |  } ]
                |}
                |""".stripMargin
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
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(BAD_REQUEST)
                  .withBody(s"""
                               | {
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
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = BAD_REQUEST,
            jsonBodyOpt = Some(
              """
                |{
                |  "correlationId" : "d677693e-9981-4ee3-8574-654981ebe606",
                |  "code" : "BAD_REQUEST",
                |  "message" : "Bad Request",
                |  "errors" : [ {
                |    "code" : "INVALID_REQUEST_PARAMETER",
                |    "message" : "007 - EORI doesn’t exist in the database"
                |  } ]
                |}
                |""".stripMargin
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
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(BAD_REQUEST)
                  .withBody(s"""
                               | {
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
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/null"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = BAD_REQUEST,
            jsonBodyOpt = Some(
              """
                |{
                |  "correlationId" : "d677693e-9981-4ee3-8574-654981ebe606",
                |  "code" : "BAD_REQUEST",
                |  "message" : "Bad Request",
                |  "errors" : [ {
                |    "code" : "INVALID_REQUEST_PARAMETER",
                |    "message" : "025 - Invalid request parameter recordId"
                |  },
                |   {
                |    "code" : "INVALID_REQUEST_PARAMETER",
                |    "message" : "026 - recordId does not exist in the database"
                |  }]
                |}
                |""".stripMargin
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
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(BAD_REQUEST)
                  .withBody(s"""
                               | {
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
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = BAD_REQUEST,
            jsonBodyOpt = Some(
              """
                {
                |  "correlationId" : "d677693e-9981-4ee3-8574-654981ebe606",
                |  "code" : "BAD_REQUEST",
                |  "message" : "Bad Request",
                |  "errors" : [ {
                |    "code" : "UNEXPECTED_ERROR",
                |    "message" : "Unexpected Error"
                |  } ]
                |}
                |""".stripMargin
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
              .withHeader("X-Client-Id", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withStatus(BAD_REQUEST)
                  .withBody(s"""
                               | {
                               |    "timestamp": "2023-09-14T11:29:18Z",
                               |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
                               |    "errorCode": "400",
                               |    "errorMessage": "Invalid request parameter",
                               |    "source": "BACKEND",
                               |    "sourceFaultDetail": {
                               |      "detail": ["error"]
                               |    }
                               |  }
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = INTERNAL_SERVER_ERROR,
            jsonBodyOpt = Some(
              """
                |{
                |  "statusCode" : 500,
                |  "message" : "Unable to parse fault detail: error"
                |}
                |""".stripMargin
            )
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
              .withHeader("X-Client-Id", equalTo("tss"))
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
              .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
              .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-Id", "tss"))
              .get()
          )

          assertAsExpected(
            response = response,
            status = BAD_REQUEST,
            jsonBodyOpt = Some(
              """
                | {
                |  "correlationId" : "d677693e-9981-4ee3-8574-654981ebe606",
                |  "code" : "UNEXPECTED_ERROR",
                |  "message" : "Unexpected Error"
                | }
                |""".stripMargin
            )
          )
          verifyThatDownstreamApiWasCalled()
        }
      }
      "invalid with missing mandatory header" in {

        val response = wsClient
          .url(fullUrl("/GB123456789001/records/8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"))
          .withHttpHeaders(("Content-Type", "application/json"))
          .get()
          .futureValue

        assertAsExpected(
          response = response,
          status = BAD_REQUEST,
          jsonBodyOpt = Some(
            """
              |{
              |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
              |    "code": "BAD_REQUEST",
              |    "message": "Missing mandatory header X-Client-Id"
              |}
              |""".stripMargin
          )
        )
        verifyThatDownstreamApiWasNotCalled()
      }
    }
  }
}
