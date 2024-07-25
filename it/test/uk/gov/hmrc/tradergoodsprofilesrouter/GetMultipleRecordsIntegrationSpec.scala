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
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import sttp.model.Uri.UriContext
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.GetRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{AuthTestSupport, HawkIntegrationSpec}

import java.time.Instant

class GetMultipleRecordsIntegrationSpec extends HawkIntegrationSpec with AuthTestSupport with BeforeAndAfterEach {

  private val eori                       = "GB123456789001"
  private val correlationId              = "d677693e-9981-4ee3-8574-654981ebe606"
  private val dateTime                   = Instant.parse("2021-12-17T09:30:47Z")
  private val timestamp                  = "Fri, 17 Dec 2021 09:30:47 GMT"
  override def hawkConnectorPath: String = s"/tgp/getrecords/v1"
  private val url                        = fullUrl(s"/traders/$eori/records")

  override def beforeEach(): Unit = {
    reset(authConnector)
    withAuthorizedTrader()
    super.beforeEach()
    when(uuidService.uuid).thenReturn(correlationId)
    when(dateTimeService.timestamp).thenReturn(dateTime)
  }

  "attempting to get records, when" - {
    "the request is" - {

      "valid without optional query parameter" in {
        stubForEis(OK, Some(getMultipleRecordEisResponseData.toString()))

        val response = wsClient
          .url(url)
          .withHttpHeaders(
            ("X-Client-ID", "tss"),
            ("Accept", "application/vnd.hmrc.1.0+json"),
            ("Accept", "application/vnd.hmrc.1.0+json")
          )
          .get()
          .futureValue

        response.status shouldBe OK
        response.json   shouldBe Json.toJson(getMultipleRecordResponseData.as[GetRecordsResponse])

        verifyThatDownstreamApiWasCalled(hawkConnectorPath)
      }
      "valid with optional query parameter lastUpdatedDate, page and size" in {
        stubForEis(OK, Some(getMultipleRecordEisResponseData.toString()), Some(dateTime.toString), Some(1), Some(1))

        val response = wsClient
          .url(fullUrl(s"/traders/$eori/records/?lastUpdatedDate=$dateTime&page=1&size=1"))
          .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
          .get()
          .futureValue

        response.status shouldBe OK
        response.json   shouldBe Json.toJson(getMultipleRecordResponseData.as[GetRecordsResponse])

        verifyThatDownstreamApiWasCalled(hawkConnectorPath)
      }
      "valid with optional query parameter page and size" in {
        stubForEis(OK, Some(getMultipleRecordEisResponseData.toString()), None, Some(1), Some(1))

        val response = wsClient
          .url(fullUrl(s"/traders/$eori/records?page=1&size=1"))
          .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
          .get()
          .futureValue

        response.status shouldBe OK
        response.json   shouldBe Json.toJson(getMultipleRecordResponseData.as[GetRecordsResponse])

        verifyThatDownstreamApiWasCalled(hawkConnectorPath)
      }
      "valid with optional query parameter page" in {
        stubForEis(OK, Some(getMultipleRecordEisResponseData.toString()), None, Some(1), None)

        val response = wsClient
          .url(fullUrl(s"/traders/$eori/records?page=1"))
          .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
          .get()
          .futureValue

        response.status shouldBe OK
        response.json   shouldBe Json.toJson(getMultipleRecordResponseData.as[GetRecordsResponse])

        verifyThatDownstreamApiWasCalled(hawkConnectorPath)
      }
      "valid with optional query parameter lastUpdatedDate" in {
        stubForEis(OK, Some(getMultipleRecordEisResponseData.toString()), Some(dateTime.toString))

        val response = wsClient
          .url(fullUrl(s"/traders/$eori/records?lastUpdatedDate=$dateTime"))
          .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
          .get()
          .futureValue

        response.status shouldBe OK
        response.json   shouldBe Json.toJson(getMultipleRecordResponseData.as[GetRecordsResponse])

        verifyThatDownstreamApiWasCalled(hawkConnectorPath)
      }
      "valid but the integration call fails with response:" - {
        "Forbidden" in {
          stubForEis(FORBIDDEN)

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
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
          stubForEis(NOT_FOUND)

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

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

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

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

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

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

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

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

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

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

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

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

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

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

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

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

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

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

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_GATEWAY",
            "message"       -> "Bad Gateway"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 503 errorCode" in {
          stubForEis(INTERNAL_SERVER_ERROR, Some(eisErrorResponse("503", "Service Unavailable")))

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

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

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

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

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNEXPECTED_ERROR",
            "message"       -> "Unexpected Error"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }

        "Bad Request with unexpected error" in {
          stubFor(
            get(urlEqualTo(s"$hawkConnectorPath/$eori"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 GMT"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("Bearer c29tZS10b2tlbgo="))
              .withHeader("X-Client-ID", equalTo("tss"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
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

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

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
          stubFor(
            get(urlEqualTo(s"$hawkConnectorPath/$eori"))
              .willReturn(
                aResponse()
                  .withHeader("Content-Type", "application/json")
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
                               |      "detail": ["error"]
                               |    }
                               |  }
                               |}
                               |""".stripMargin)
              )
          )

          val response = await(
            wsClient
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

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
            get(urlEqualTo(s"$hawkConnectorPath/$eori"))
              .withHeader("X-Forwarded-Host", equalTo("MDTP"))
              .withHeader("X-Correlation-ID", equalTo("d677693e-9981-4ee3-8574-654981ebe606"))
              .withHeader("Date", equalTo("Fri, 17 Dec 2021 09:30:47 GMT"))
              .withHeader("Accept", equalTo("application/json"))
              .withHeader("Authorization", equalTo("Bearer c29tZS10b2tlbgo="))
              .withHeader("X-Client-ID", equalTo("tss"))
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
              .url(url)
              .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
              .get()
          )

          response.status shouldBe BAD_REQUEST
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNEXPECTED_ERROR",
            "message"       -> "Unexpected Error"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
      }
      "invalid with missing mandatory header" in {

        val response = wsClient
          .url(url)
          .get()
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
      "forbidden with any of the following" - {
        "EORI number is not authorized" in {

          val response = wsClient
            .url(fullUrl(s"/traders/GB123456789015/records"))
            .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
            .get()
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
            .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
            .get()
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

    "should return an error if lastUpdateDate is not a date" in {
      stubForEis(OK, Some(getMultipleRecordEisResponseData.toString()))

      val response = wsClient
        .url(fullUrl(s"/traders/$eori/records?lastUpdatedDate=wrong-format"))
        .withHttpHeaders(("X-Client-ID", "tss"), ("Accept", "application/vnd.hmrc.1.0+json"))
        .get()
        .futureValue

      response.status shouldBe BAD_REQUEST
      response.json   shouldBe Json.obj(
        "correlationId" -> s"$correlationId",
        "code"          -> "INVALID_QUERY_PARAMETER",
        "message"       -> "Query parameter lastUpdateDate is not a date format"
      )

    }
  }

  private def stubForEis(
    httpStatus: Int,
    body: Option[String] = None,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  ) = {
    val uri =
      uri"$hawkConnectorPath/$eori?lastUpdatedDate=$lastUpdatedDate&page=$page&size=$size"

    stubFor(
      get(urlEqualTo(s"$uri"))
        .withHeader("X-Forwarded-Host", equalTo("MDTP"))
        .withHeader("X-Correlation-ID", equalTo(correlationId))
        .withHeader("Date", equalTo(timestamp))
        .withHeader("Accept", equalTo("application/json"))
        .withHeader("Authorization", equalTo("Bearer c29tZS10b2tlbgo="))
        .withHeader("X-Client-ID", equalTo("tss"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(httpStatus)
            .withBody(body.orNull)
        )
    )
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

  def getMultipleRecordEisResponseData: JsValue =
    Json.parse(s"""
    |{
    |"goodsItemRecords":
    |[
    |  {
    |    "eori": "$eori",
    |    "actorId": "GB1234567890",
    |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
    |    "traderRef": "BAN001001",
    |    "comcode": "10410100",
    |    "accreditationStatus": "Approved",
    |    "goodsDescription": "Organic bananas",
    |    "countryOfOrigin": "EC",
    |    "category": 3,
    |    "assessments": [
    |      {
    |        "assessmentId": "abc123",
    |        "primaryCategory": 1,
    |        "condition": {
    |          "type": "abc123",
    |          "conditionId": "Y923",
    |          "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
    |          "conditionTraderText": "Excluded product"
    |        }
    |      }
    |    ],
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
    |    "createdDateTime": "2024-11-18T23:20:19Z",
    |    "updatedDateTime": "2024-11-18T23:20:19Z"
    |  },
    |    {
    |    "eori": "$eori",
    |    "actorId": "GB1234567890",
    |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
    |    "traderRef": "BAN001001",
    |    "comcode": "10410100",
    |    "accreditationStatus": "Rejected",
    |    "goodsDescription": "Organic bananas",
    |    "countryOfOrigin": "EC",
    |    "category": 3,
    |    "assessments": [
    |      {
    |        "assessmentId": "abc123",
    |        "primaryCategory": 1,
    |        "condition": {
    |          "type": "abc123",
    |          "conditionId": "Y923",
    |          "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
    |          "conditionTraderText": "Excluded product"
    |        }
    |      }
    |    ],
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
    |    "createdDateTime": "2024-11-18T23:20:19Z",
    |    "updatedDateTime": "2024-11-18T23:20:19Z"
    |  }
    |],
    |"pagination":
    | {
    |   "totalRecords": 2,
    |   "currentPage": 0,
    |   "totalPages": 1,
    |   "nextPage": null,
    |   "prevPage": null
    | }
    |}
    |""".stripMargin)

  def getMultipleRecordResponseData: JsValue =
    Json.parse(s"""
                  |{
                  |"goodsItemRecords":
                  |[
                  |  {
                  |    "eori": "$eori",
                  |    "actorId": "GB1234567890",
                  |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
                  |    "traderRef": "BAN001001",
                  |    "comcode": "10410100",
                  |    "adviceStatus": "Advice Provided",
                  |    "goodsDescription": "Organic bananas",
                  |    "countryOfOrigin": "EC",
                  |    "category": 3,
                  |    "assessments": [
                  |      {
                  |        "assessmentId": "abc123",
                  |        "primaryCategory": 1,
                  |        "condition": {
                  |          "type": "abc123",
                  |          "conditionId": "Y923",
                  |          "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
                  |          "conditionTraderText": "Excluded product"
                  |        }
                  |      }
                  |    ],
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
                  |    "createdDateTime": "2024-11-18T23:20:19Z",
                  |    "updatedDateTime": "2024-11-18T23:20:19Z"
                  |  },
                  |    {
                  |    "eori": "$eori",
                  |    "actorId": "GB1234567890",
                  |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
                  |    "traderRef": "BAN001001",
                  |    "comcode": "10410100",
                  |    "adviceStatus": "Advice not provided",
                  |    "goodsDescription": "Organic bananas",
                  |    "countryOfOrigin": "EC",
                  |    "category": 3,
                  |    "assessments": [
                  |      {
                  |        "assessmentId": "abc123",
                  |        "primaryCategory": 1,
                  |        "condition": {
                  |          "type": "abc123",
                  |          "conditionId": "Y923",
                  |          "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
                  |          "conditionTraderText": "Excluded product"
                  |        }
                  |      }
                  |    ],
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
                  |    "createdDateTime": "2024-11-18T23:20:19Z",
                  |    "updatedDateTime": "2024-11-18T23:20:19Z"
                  |  }
                  |],
                  |"pagination":
                  | {
                  |   "totalRecords": 2,
                  |   "currentPage": 0,
                  |   "totalPages": 1,
                  |   "nextPage": null,
                  |   "prevPage": null
                  | }
                  |}
                  |""".stripMargin)
}
