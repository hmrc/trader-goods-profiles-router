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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{AuthTestSupport, HawkIntegrationSpec}

import java.time.Instant

class GetProfileIntegrationSpec extends HawkIntegrationSpec with AuthTestSupport with BeforeAndAfterEach{

  private val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  private val ukimsNumber = "AC123456789124"
  private val nirmsNumber = "1234567891234"
  private val niphlNumber = "12345678"
  private val timestamp = Instant.parse("2021-12-17T09:30:47.45Z")
  private val url           = fullUrl(s"/customs/traders/goods-profiles/$eoriNumber")
  override def hawkConnectorPath: String = "/tgp/getprofile/v1"

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(authConnector)

    when(uuidService.uuid).thenReturn(correlationId)
    when(dateTimeService.timestamp).thenReturn(timestamp)
  }

  "Get Profile" - {
    "return 200 with the profile response" in {
      withAuthorizedTrader()
      stubEisRequest(200, createProfileResponse.toString())

      val response = sendAndWait

      response.status shouldBe OK
      response.json shouldBe createProfileResponse
    }

    "return an error for an unauthorised eori" in {
      withUnauthorizedTrader(new RuntimeException("Error"))
      val response = sendAndWait

      response.status shouldBe INTERNAL_SERVER_ERROR
    }

    "return an error" - {
      "when EIS return 403" in {
        withAuthorizedTrader()
        stubEisRequest(403)

        val response = sendAndWait

        response.status shouldBe FORBIDDEN
         response.json shouldBe Json.obj(
           "correlationId" -> correlationId,
           "code" -> "FORBIDDEN",
           "message" -> "Forbidden"
         )
      }

      "EIS return 404" in {
        withAuthorizedTrader()
        stubEisRequest(404)

        val response = sendAndWait

        response.status shouldBe NOT_FOUND
        response.json shouldBe Json.obj(
          "correlationId" -> correlationId,
          "code" -> "NOT_FOUND",
          "message" -> "Not Found"
        )
      }

      "when EIS return 400 with valid errors payload" in {
        withAuthorizedTrader()
        stubEisRequest(400, Eis400ErrorResponse.toString())

        val response = sendAndWait

        response.status shouldBe BAD_REQUEST
        response.json shouldBe expectedResponseForEIS400Error
      }

      "when EIS return 400 and eori does not exist" in {
        withAuthorizedTrader()
        stubEisRequest(400, Eis500ErrorResponseForEoriNotExist.toString())

        val response = sendAndWait

        response.status shouldBe BAD_REQUEST
        response.json shouldBe  Json.obj(
          "correlationId" -> correlationId,
          "code" -> "BAD_REQUEST",
          "message" -> "Bad Request",
          "errors" -> Json.arr(
            Json.obj(
              "code" -> "INVALID_REQUEST_PARAMETER",
              "message" -> "EORI number does not have a TGP",
              "errorNumber" -> 7
            )
          )
        )
      }

      "When EIS return 400 and error response contains unsupported code" in {
        withAuthorizedTrader()
        stubEisRequest(400, Eis400ErrorResponseWithUnsupportedMessage.toString())

        val response = sendAndWait

        response.status shouldBe BAD_REQUEST

        response.json shouldBe  Json.obj(
          "correlationId" -> correlationId,
          "code" -> "BAD_REQUEST",
          "message" -> "Bad Request",
          "errors" -> Json.arr(
            Json.obj(
              "code" -> "UNEXPECTED_ERROR",
              "message" -> "Unrecognised error number",
              "errorNumber" -> 123
            )
          )
        )
      }


      val table = Table(
        ("description", "code", "message", "expectedCode"),
        ("with error code 200", "200", "Invalid Response Payload or Empty payload", "INVALID_OR_EMPTY_PAYLOAD"),
        ("with error code 401", "401", "Unauthorized", "UNAUTHORIZED"),
        ("with error code 404", "404", "Not Found", "NOT_FOUND"),
        ("with error code 405", "405", "Method Not Allowed", "METHOD_NOT_ALLOWED"),
        ("with error code 500", "500", "Internal Server Error", "INTERNAL_SERVER_ERROR"),
        ("with error code 502", "502", "Bad Gateway", "BAD_GATEWAY"),
        ("with error code 503", "503", "Service Unavailable", "SERVICE_UNAVAILABLE")
      )

      forAll(table) {
        (
          description: String,
          code: String,
          message: String,
          expectedCode: String
        ) => s"when EIS return 500 $description" in {

          withAuthorizedTrader()
          stubEisRequest(500, Eis500ErrorResponseWithoutErrors(code, message).toString())

          val response = sendAndWait

          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json shouldBe  Json.obj(
            "correlationId" -> correlationId,
            "code" -> expectedCode,
            "message" -> message
          )
        }
      }
    }
  }

  private def sendAndWait = {
    await(wsClient
      .url(url)
      .withHttpHeaders(Seq("Accept" -> "application/vnd.hmrc.1.0+json"): _*)
      .get()
    )
  }

  private def expectedResponseForEIS400Error = {
    Json.obj(
      "correlationId" -> correlationId,
      "code" -> "BAD_REQUEST",
      "message" -> "Bad Request",
      "errors" -> Json.arr(
        Json.obj(
          "code" -> "INVALID_REQUEST_PARAMETER",
          "message" -> "JSON request body is unreadable",
          "errorNumber" -> 0
        ),
        Json.obj(
          "code" -> "INVALID_REQUEST_PARAMETER",
          "message" -> "X-Correlation-ID was missing from Header or is in the wrong format",
          "errorNumber" -> 1
        ),
        Json.obj(
          "code" -> "INVALID_REQUEST_PARAMETER",
          "message" -> "Request Date was missing from Header or is in the wrong format",
          "errorNumber" -> 2
        ),
        Json.obj(
          "code" -> "INVALID_REQUEST_PARAMETER",
          "message" -> "Content-Type was missing from Header or is in the wrong format",
          "errorNumber" -> 3
        ),
        Json.obj(
          "code" -> "INVALID_REQUEST_PARAMETER",
          "message" -> "Accept was missing from Header or is in the wrong format",
          "errorNumber" -> 4
        ),
        Json.obj(
          "code" -> "INVALID_REQUEST_PARAMETER",
          "message" -> "X-Forwarded-Host was missing from Header or is in the wrong format",
          "errorNumber" -> 5
        ),
        Json.obj(
          "code" -> "INVALID_REQUEST_PARAMETER",
          "message" -> "Mandatory field eori was missing from body or is in the wrong format",
          "errorNumber" -> 6
        )
      )
    )
  }

  private def Eis400ErrorResponse: JsValue = {
    Json.parse(
      s"""
         |{
         |  "errorDetail":{
         |    "timestamp":"${timestamp.toString}",
         |    "correlationId":"$correlationId",
         |    "errorCode": "400",
         |    "errorMessage":"Bad Request",
         |    "source":"BACKEND",
         |    "sourceFaultDetail":{
         |      "detail":[
         |        "error: 000, message: JSON request body is unreadable",
         |        "error: 001, message: Missing or Invalid Header",
         |        "error: 002, message: Missing or Invalid Header",
         |        "error: 003, message: Missing or Invalid Header",
         |        "error: 004, message: Missing or Invalid Header",
         |        "error: 005, message: Missing or Invalid Header",
         |        "error: 006, message: Missing or Invalid mandatory request parameter"
         |      ]
         |     }
         |  }
         |}
         |""".stripMargin)
  }

  private def Eis400ErrorResponseWithUnsupportedMessage: JsValue = {
    Json.parse(
      s"""
         |{
         |  "errorDetail":{
         |    "timestamp":"${timestamp.toString}",
         |    "correlationId":"$correlationId",
         |    "errorCode": "400",
         |    "errorMessage":"Bad Request",
         |    "source":"BACKEND",
         |    "sourceFaultDetail":{
         |      "detail":[
         |        "error: 123, message: invalid messge"
         |      ]
         |     }
         |  }
         |}
         |""".stripMargin)
  }

  private def Eis500ErrorResponseForEoriNotExist: JsValue = {
    Json.parse(
      s"""
         |{
         |  "errorDetail":{
         |    "timestamp":"${timestamp.toString}",
         |    "correlationId":"$correlationId",
         |    "errorCode": "400",
         |    "errorMessage":"Bad Request",
         |    "source":"BACKEND",
         |    "sourceFaultDetail":{
         |      "detail":[
         |        "error: 007, message: eori doesn't exist in the database"
         |      ]
         |     }
         |  }
         |}
         |""".stripMargin)
  }

  private def Eis500ErrorResponseWithoutErrors(code: String, message: String): JsValue = {
    Json.parse(
      s"""
         |{
         |  "errorDetail":{
         |    "timestamp":"${timestamp.toString}",
         |    "correlationId":"$correlationId",
         |    "errorCode": "$code",
         |    "errorMessage":"$message",
         |    "source":"BACKEND",
         |    "sourceFaultDetail":{
         |      "detail":[]
         |     }
         |  }
         |}
         |""".stripMargin)
  }

  private def stubEisRequest(status: Int, responseBody: String): Any = {
    stubFor(
      get(urlEqualTo(s"$hawkConnectorPath/$eoriNumber"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(responseBody)

        )
    )
  }

  private def stubEisRequest(status: Int): Any = {
    stubFor(
      get(urlEqualTo(s"$hawkConnectorPath/$eoriNumber"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )
  }

  private def createProfileResponse: JsObject = {
    Json.obj(
      "eori" -> eoriNumber,
      "actorId" -> "actorId",
      "ukimsNumber" -> ukimsNumber,
      "nirmsNumber" -> nirmsNumber,
      "niphlNumber" -> niphlNumber
    )
  }
}
