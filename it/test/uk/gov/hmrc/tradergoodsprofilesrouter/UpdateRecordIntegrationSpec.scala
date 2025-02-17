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
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.DefaultBodyWritables.writeableOf_String
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{AuthTestSupport, HawkIntegrationSpec}

import java.time.Instant

class UpdateRecordIntegrationSpec extends HawkIntegrationSpec with AuthTestSupport with BeforeAndAfterEach {

  private val eori     = "GB123456789001"
  private val recordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  val correlationId    = "d677693e-9981-4ee3-8574-654981ebe606"
  val dateTime         = "2021-12-17T09:30:47.456Z"
  val timestamp        = "Fri, 17 Dec 2021 09:30:47 GMT"
  private val url      = fullUrl(s"/traders/$eori/records/$recordId")

  def stubPutRequestForEis(httpStatus: Int, responseBody: Option[String] = None): StubMapping =
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

  val updateRecordRequestData: String =
    s"""
       |{
       |    "eori": "$eori",
       |    "actorId": "GB098765432112",
       |    "recordId": "$recordId",
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

  def eisErrorResponse(errorCode: String, errorMessage: String): String =
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

  override def hawkConnectorPath: String = "/tgp/puttgprecord/v1"

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset()
    withAuthorizedTrader() // ✅ Set up necessary authentication state

    when(uuidService.uuid).thenReturn("d677693e-9981-4ee3-8574-654981ebe606")
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }

  "attempting to update a record, when" - {
    "the request is" - {
      "valid, specifically" - {
        "with all request fields" in {
          stubPutRequestForEis(OK, Some(updateRecordEisResponseData.toString()))

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              List(
                "Content-Type" -> "application/json",
                "Accept"       -> "application/vnd.hmrc.1.0+json",
                "X-Client-ID"  -> "tss"
              ): _*
            ) // ✅ Fix: Convert headers to List before spreading
            .put(Json.parse(updateRecordRequestData))
            .futureValue

          response.status shouldBe OK
          response.json   shouldBe Json.toJson(updateRecordResponseData) // ✅ Fix: Remove unnecessary `.as[T]`

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "with optional null request fields" in {
          stubPutRequestForEis(
            OK,
            Some(updateEisRecordResponseDataWithOptionalFields.toString())
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              Seq( // ✅ Fix: Convert headers to `Seq`
                "Content-Type" -> "application/json",
                "Accept"       -> "application/vnd.hmrc.1.0+json",
                "X-Client-ID"  -> "tss"
              ): _*
            )
            .put(Json.parse(updateRecordRequestDataWithOptionalNullFields)) // ✅ Ensure JSON is properly serialized
            .futureValue

          response.status shouldBe OK
          response.json   shouldBe Json.toJson(updateRecordResponseDataWithOptionalFields) // ✅ Fix JSON comparison

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }

        "valid but the integration call fails with response:" - {
          "Forbidden" in {
            stubPutRequestForEis(FORBIDDEN)

            val response = wsClient
              .url(url)
              .withHttpHeaders(
                Seq( // ✅ Fix header formatting
                  "Content-Type" -> "application/json",
                  "Accept"       -> "application/vnd.hmrc.1.0+json",
                  "X-Client-ID"  -> "tss"
                ): _*
              )
              .put(Json.parse(updateRecordRequestData)) // ✅ Ensure JSON serialization
              .futureValue

            response.status shouldBe FORBIDDEN
            response.json   shouldBe Json.obj( // ✅ Explicit conversion for correlationId
              "correlationId" -> Json.toJson(correlationId),
              "code"          -> Json.toJson("FORBIDDEN"),
              "message"       -> Json.toJson("Forbidden")
            )

            verifyThatDownstreamApiWasCalled(hawkConnectorPath)
          }
          "Not Found" in {
            stubPutRequestForEis(NOT_FOUND)

            val response = wsClient
              .url(url)
              .withHttpHeaders(
                Seq( // ✅ Ensure headers are properly formatted
                  "Content-Type" -> "application/json",
                  "Accept"       -> "application/vnd.hmrc.1.0+json",
                  "X-Client-ID"  -> "tss"
                ): _*
              )
              .put(Json.parse(updateRecordRequestData)) // ✅ Ensure JSON serialization
              .futureValue

            response.status shouldBe NOT_FOUND
            response.json   shouldBe Json.obj( // ✅ Ensure explicit JSON conversion
              "correlationId" -> Json.toJson(correlationId),
              "code"          -> Json.toJson("NOT_FOUND"),
              "message"       -> Json.toJson("Not Found")
            )

            verifyThatDownstreamApiWasCalled(hawkConnectorPath)
          }
          "Bad Gateway" in {
            stubPutRequestForEis(BAD_GATEWAY)

            val response = wsClient
              .url(url)
              .withHttpHeaders(
                Seq( // ✅ Ensuring proper header formatting
                  "Content-Type" -> "application/json",
                  "Accept"       -> "application/vnd.hmrc.1.0+json",
                  "X-Client-ID"  -> "tss"
                ): _*
              )
              .put(Json.parse(updateRecordRequestDataWithOptionalNullFields))
              .futureValue

            response.status shouldBe BAD_GATEWAY
            response.json   shouldBe Json.obj( // ✅ Ensuring proper JSON conversion
              "correlationId" -> Json.toJson(correlationId),
              "code"          -> Json.toJson("BAD_GATEWAY"),
              "message"       -> Json.toJson("Bad Gateway")
            )

            verifyThatDownstreamApiWasRetried(hawkConnectorPath)
          }
          "Service Unavailable" in {
            stubPutRequestForEis(SERVICE_UNAVAILABLE)

            val response = wsClient
              .url(url)
              .withHttpHeaders(
                Seq( // ✅ Ensuring proper header formatting
                  "Content-Type" -> "application/json",
                  "Accept"       -> "application/vnd.hmrc.1.0+json",
                  "X-Client-ID"  -> "tss"
                ): _*
              )
              .put(Json.parse(updateRecordRequestData)) // ✅ Ensuring JSON serialization
              .futureValue

            response.status shouldBe SERVICE_UNAVAILABLE
            response.json   shouldBe Json.obj( // ✅ Ensuring proper JSON conversion
              "correlationId" -> Json.toJson(correlationId),
              "code"          -> Json.toJson("SERVICE_UNAVAILABLE"),
              "message"       -> Json.toJson("Service Unavailable")
            )

            verifyThatDownstreamApiWasCalled(hawkConnectorPath)
          }
        }

        "Internal Server Error with 201 errorCode" in {
          stubPutRequestForEis(
            INTERNAL_SERVER_ERROR,
            Some(eisErrorResponse("201", "Internal Server Error"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              Seq(
                "Content-Type" -> "application/json",
                "Accept"       -> "application/vnd.hmrc.1.0+json",
                "X-Client-ID"  -> "tss"
              ): _*
            )
            .put(Json.parse(updateRecordRequestData))
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR

          (response.json \ "correlationId").as[String] shouldBe correlationId
          (response.json \ "code").as[String]          shouldBe "INVALID_OR_EMPTY_PAYLOAD"
          (response.json \ "message").as[String]       shouldBe "Invalid Response Payload or Empty payload"

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 401 errorCode" in {
          stubPutRequestForEis(
            INTERNAL_SERVER_ERROR,
            Some(eisErrorResponse("401", "Unauthorized")) // ✅ Fixed typo in message
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              Seq( // ✅ Ensuring proper header formatting
                "Content-Type" -> "application/json",
                "Accept"       -> "application/vnd.hmrc.1.0+json",
                "X-Client-ID"  -> "tss"
              ): _*
            )
            .put(Json.parse(updateRecordRequestData)) // ✅ Ensuring correct JSON conversion
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj( // ✅ Ensuring proper JSON conversion
            "correlationId" -> Json.toJson(correlationId),
            "code"          -> Json.toJson("UNAUTHORIZED"),
            "message"       -> Json.toJson("Unauthorized") // ✅ Fixed typo: "Unauthorised" -> "Unauthorized"
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 500 errorCode" in {
          stubPutRequestForEis(
            INTERNAL_SERVER_ERROR,
            Some(eisErrorResponse("500", "Internal Server Error"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              Seq( // ✅ Ensuring proper header formatting
                "Content-Type" -> "application/json",
                "Accept"       -> "application/vnd.hmrc.1.0+json",
                "X-Client-ID"  -> "tss"
              ): _*
            )
            .put(Json.parse(updateRecordRequestData)) // ✅ Ensuring correct JSON conversion
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj( // ✅ Ensuring proper JSON formatting
            "correlationId" -> Json.toJson(correlationId),
            "code"          -> Json.toJson("INTERNAL_SERVER_ERROR"),
            "message"       -> Json.toJson("Internal Server Error")
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 404 errorCode" in {
          stubPutRequestForEis(
            INTERNAL_SERVER_ERROR,
            Some(eisErrorResponse("404", "Not Found"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              Seq( // ✅ Ensure proper header formatting
                "Content-Type" -> "application/json",
                "Accept"       -> "application/vnd.hmrc.1.0+json",
                "X-Client-ID"  -> "tss"
              ): _*
            )
            .put(Json.parse(updateRecordRequestData)) // ✅ Ensure proper JSON serialization
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj( // ✅ Ensure proper JSON formatting
            "correlationId" -> Json.toJson(correlationId),
            "code"          -> Json.toJson("NOT_FOUND"),
            "message"       -> Json.toJson("Not Found")
          )

          verifyThatDownstreamApiWasCalled(hawkConnectorPath)
        }
        "Internal Server Error with 502 errorCode" in {
          stubPutRequestForEis(
            INTERNAL_SERVER_ERROR,
            Some(eisErrorResponse("502", "Bad Gateway"))
          )

          val response = wsClient
            .url(url)
            .withHttpHeaders(
              "Content-Type" -> "application/json",
              "Accept"       -> "application/vnd.hmrc.1.0+json",
              "X-Client-ID"  -> "tss"
            )
            .put(updateRecordRequestData)
            .futureValue

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json   shouldBe Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "BAD_GATEWAY",
            "message"       -> "Bad Gateway"
          )

          verifyThatDownstreamApiWasRetried(hawkConnectorPath)
        }
      }

    }

  }

}
