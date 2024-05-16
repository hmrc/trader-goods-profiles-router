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

package uk.gov.hmrc.tradergoodsprofilesrouter.connectors

import org.scalatest.EitherValues
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.{BadRequest, InternalServerError}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISHttpReader.responseHandler
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport

class EISHttpReaderSpec extends PlaySpec with GetRecordsDataSupport with EitherValues {

  implicit val correlationId: String = "1234-456"
  "responseHandler" should {
    "return a record item" in {
      val eisResponse = HttpResponse(200, getEisRecordsResponseData, Map.empty)

      val result = await(responseHandler(eisResponse))

      result.value mustBe getEisRecordsResponseData.as[GetEisRecordsResponse]
    }

    "return an internal server error" when {
      "Invalid payload response" in {
        val eisResponse: String = createEisErrorResponseAsJson("200", "Internal Server Error")
        val eisHttpResponse     = HttpResponse(500, eisResponse)

        val result = await(responseHandler(eisHttpResponse))

        result.left.value.header.status mustBe INTERNAL_SERVER_ERROR
        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson(
            "INVALID_OR_EMPTY_PAYLOAD",
            "Invalid Response Payload or Empty payload"
          )
        )
      }
      "Payload schema mismatch" in {
        val eisResponse     = createEisErrorResponseAsJson("400", "Internal Error Response")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = await(responseHandler(eisHttpResponse))

        result.left.value.header.status mustBe INTERNAL_SERVER_ERROR
        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson(
            "INTERNAL_ERROR_RESPONSE",
            "Internal Error Response"
          )
        )
      }
      "Unauthorised" in {
        val eisResponse     = createEisErrorResponseAsJson("401", "Unauthorised")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = await(responseHandler(eisHttpResponse))

        result.left.value.header.status mustBe INTERNAL_SERVER_ERROR
        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson(
            "UNAUTHORIZED",
            "Unauthorized"
          )
        )
      }
      "Not found" in {
        val eisResponse     = createEisErrorResponseAsJson("404", "Not Found")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = await(responseHandler(eisHttpResponse))

        result.left.value.header.status mustBe INTERNAL_SERVER_ERROR
        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson(
            "NOT_FOUND",
            "Not Found"
          )
        )
      }
      "Method not allowed" in {
        val eisResponse     = createEisErrorResponseAsJson("405", "Method Not Allowed")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = await(responseHandler(eisHttpResponse))

        result.left.value.header.status mustBe INTERNAL_SERVER_ERROR
        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson(
            "METHOD_NOT_ALLOWED",
            "Method Not Allowed"
          )
        )
      }
      "Internal server error" in {
        val eisResponse     = createEisErrorResponseAsJson("500", "Internal Server Error")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = await(responseHandler(eisHttpResponse))

        result.left.value.header.status mustBe INTERNAL_SERVER_ERROR
        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson("INTERNAL_SERVER_ERROR", "Internal Server Error")
        )

      }
      "Bad gateway" in {
        val eisResponse     = createEisErrorResponseAsJson("502", "Bad Gateway")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = await(responseHandler(eisHttpResponse))

        result.left.value.header.status mustBe INTERNAL_SERVER_ERROR
        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson("BAD_GATEWAY", "Bad Gateway")
        )

      }
      "Service unavailable" in {
        val eisResponse     = createEisErrorResponseAsJson("503", "Service Unavailable")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = await(responseHandler(eisHttpResponse))

        result.left.value.header.status mustBe INTERNAL_SERVER_ERROR
        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson("SERVICE_UNAVAILABLE", "Service Unavailable")
        )
      }
      "Unknown error response" in {
        val eisResponse     = createEisErrorResponseAsJson("001", "Service Unavailable")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = await(responseHandler(eisHttpResponse))

        result.left.value.header.status mustBe INTERNAL_SERVER_ERROR
        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson("UNKNOWN", "Unknown Error")
        )
      }
      "Unexpected error is thrown" in {
        val invalidJson     = """{ "wrongField": "value" }"""
        val eisHttpResponse = HttpResponse(500, invalidJson)

        val result = await(responseHandler(eisHttpResponse))

        result.left.value.header.status mustBe INTERNAL_SERVER_ERROR
        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson("UNEXPECTED_ERROR", "Unexpected Error")
        )
      }
    }

    "return an bad request error" when {
      "eori does not exist and comcode is missing" in {
        val eisResponse  = createEisErrorResponseWithDetailsAsJson(
          "400",
          "Internal Server Error",
          Seq(
            "error: 006, message: Mandatory field comcode was missing from body",
            "error: 007, message: eori does not exist in the database"
          ): _*
        )
        val httpResponse = HttpResponse(400, eisResponse, Map.empty)

        val result = await(responseHandler(httpResponse))

        result.left.value mustBe BadRequest(
          createErrorResponseWithErrorAsJson(
            "BAD_REQUEST",
            "Bad Request",
            Seq(
              "INVALID_REQUEST_PARAMETER" -> "006 - Missing or invalid mandatory request parameter EORI",
              "INVALID_REQUEST_PARAMETER" -> "007 - EORI does not exist in the database"
            ): _*
          )
        )
      }

      "Unexpected error response given an invalid json string" in {
        val httpResponse = HttpResponse(400, """{"invalid": "json"}""")

        val result = await(responseHandler(httpResponse))

        result.left.value mustBe BadRequest(
          createErrorResponseAsJson("UNEXPECTED_ERROR", "Unexpected Error")
        )
      }
      "Unexpected error code response" in {
        val eisResponse  =
          s"""
             |{
             |    "timestamp": "2023-09-14T11:29:18Z",
             |    "correlationId": "$correlationId",
             |    "errorCode": "400",
             |    "errorMessage": "Bad Request",
             |    "source": "BACKEND",
             |    "sourceFaultDetail": {
             |      "detail": [
             |        "error: 100, message: unknown"
             |      ]
             |    }
             |  }
        """.stripMargin
        val httpResponse = HttpResponse(400, eisResponse, Map.empty)

        val result = await(responseHandler(httpResponse))

        result.left.value mustBe BadRequest(
          createErrorResponseWithErrorAsJson(
            "BAD_REQUEST",
            "Bad Request",
            Seq("UNEXPECTED_ERROR" -> "Unexpected Error"): _*
          )
        )
      }

      "Unable to parse source fault detail" in {
        val eisResponse  =
          s"""
             |{
             |    "timestamp": "2023-09-14T11:29:18Z",
             |    "correlationId": "$correlationId",
             |    "errorCode": "400",
             |    "errorMessage": "Bad Request",
             |    "source": "BACKEND",
             |    "sourceFaultDetail": {
             |      "detail": [
             |        "002, unknown"
             |      ]
             |    }
             |  }
        """.stripMargin
        val httpResponse = HttpResponse(400, eisResponse, Map.empty)

        val exception = intercept[IllegalArgumentException] {
          await(responseHandler(httpResponse))
        }

        exception.getMessage mustBe s"Unable to parse fault detail for correlation Id: $correlationId"

      }
    }
  }

  private def createEisErrorResponseAsJson(errorCode: String, message: String) =
    s"""
       |{
       |    "timestamp": "2023-09-14T11:29:18Z",
       |    "correlationId": "$correlationId",
       |    "errorCode": "$errorCode",
       |    "errorMessage": "$message",
       |    "source": "BACKEND",
       |    "sourceFaultDetail": {
       |      "detail": null
       |    }
       |  }
        """.stripMargin

  private def createEisErrorResponseWithDetailsAsJson(errorCode: String, message: String, detail: String*) =
    s"""
       |{
       |    "timestamp": "2023-09-14T11:29:18Z",
       |    "correlationId": "$correlationId",
       |    "errorCode": "$errorCode",
       |    "errorMessage": "$message",
       |    "source": "BACKEND",
       |    "sourceFaultDetail": {
       |      "detail": ${Json.toJson(detail)}
       |    }
       |  }
        """.stripMargin

  private def createErrorResponseAsJson(code: String, message: String): JsValue =
    Json.toJson(ErrorResponse(correlationId, code, message))

  private def createErrorResponseWithErrorAsJson(code: String, message: String, errors: (String, String)*): JsValue =
    Json.toJson(
      ErrorResponse(
        correlationId,
        code,
        message,
        Some(errors.map(e => Error(e._1, e._2)))
      )
    )
}