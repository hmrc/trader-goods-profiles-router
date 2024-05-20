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
import play.api.mvc.Results.{BadRequest, Forbidden, InternalServerError, MethodNotAllowed, NotFound}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.HttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport

class EisHttpReaderSpec extends PlaySpec with GetRecordsDataSupport with EitherValues {

  val correlationId: String = "1234-456"
  "responseHandler" should {
    "return a record item" in {
      val eisResponse = HttpResponse(200, getEisRecordsResponseData, Map.empty)

      val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", eisResponse)

      result.value mustBe getEisRecordsResponseData.as[GetEisRecordsResponse]
    }

    "return an internal server error" when {
      "Invalid payload response" in {
        val eisResponse: String = createEisErrorResponseAsJson("200", "Internal Server Error")
        val eisHttpResponse     = HttpResponse(500, eisResponse)

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", eisHttpResponse)

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

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", eisHttpResponse)

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

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", eisHttpResponse)

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

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", eisHttpResponse)

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

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", eisHttpResponse)

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

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", eisHttpResponse)

        result.left.value.header.status mustBe INTERNAL_SERVER_ERROR
        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson("INTERNAL_SERVER_ERROR", "Internal Server Error")
        )

      }
      "Bad gateway" in {
        val eisResponse     = createEisErrorResponseAsJson("502", "Bad Gateway")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", eisHttpResponse)

        result.left.value.header.status mustBe INTERNAL_SERVER_ERROR
        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson("BAD_GATEWAY", "Bad Gateway")
        )

      }
      "Service unavailable" in {
        val eisResponse     = createEisErrorResponseAsJson("503", "Service Unavailable")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", eisHttpResponse)

        result.left.value.header.status mustBe INTERNAL_SERVER_ERROR
        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson("SERVICE_UNAVAILABLE", "Service Unavailable")
        )
      }
      "Unknown error response" in {
        val eisResponse     = createEisErrorResponseAsJson("001", "Service Unavailable")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", eisHttpResponse)

        result.left.value.header.status mustBe INTERNAL_SERVER_ERROR
        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson("UNKNOWN", "Unknown Error")
        )
      }
      "Unexpected error is thrown" in {
        val invalidJson     = """{ "wrongField": "value" }"""
        val eisHttpResponse = HttpResponse(500, invalidJson)

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", eisHttpResponse)

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

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", httpResponse)

        result.left.value mustBe BadRequest(
          createErrorResponseWithErrorAsJson(
            "BAD_REQUEST",
            "Bad Request",
            Seq(
              "006" -> "Mandatory field eori was missing from body",
              "007" -> "EORI number does not have a TGP"
            ): _*
          )
        )
      }

      "Unexpected error response given an invalid json string" in {
        val httpResponse = HttpResponse(400, """{"invalid": "json"}""")

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", httpResponse)

        result.left.value mustBe BadRequest(
          createErrorResponseAsJson("UNEXPECTED_ERROR", "Unexpected Error")
        )
      }
      "Unexpected error code response" in {
        val eisResponse  = createEisErrorResponseWithDetailsAsJson("400", "Bad Request", "error: 100, message: unknown")
        val httpResponse = HttpResponse(400, eisResponse, Map.empty)

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", httpResponse)

        result.left.value mustBe BadRequest(
          createErrorResponseWithErrorAsJson(
            "BAD_REQUEST",
            "Bad Request",
            Seq("UNEXPECTED_ERROR" -> "Unexpected Error"): _*
          )
        )
      }

      "Unable to parse source fault detail" in {
        val eisResponse  = createEisErrorResponseWithDetailsAsJson("400", "Bad Request", "002, unknown")
        val httpResponse = HttpResponse(400, eisResponse, Map.empty)

        val exception = intercept[IllegalArgumentException] {
          HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", httpResponse)
        }

        exception.getMessage mustBe s"Unable to parse fault detail for correlation Id: $correlationId"

      }
    }

    "return an error" when {
      "Forbidden response" in {
        val httpResponse = HttpResponse(403, "")

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", httpResponse)

        result.left.value mustBe Forbidden(
          createErrorResponseAsJson("FORBIDDEN", "Forbidden")
        )

      }
      "Not found response" in {
        val httpResponse = HttpResponse(404, "")

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", httpResponse)

        result.left.value mustBe NotFound(
          createErrorResponseAsJson("NOT_FOUND", "Not Found")
        )
      }
      "Method not allowed response" in {
        val httpResponse = HttpResponse(405, "")

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", httpResponse)

        result.left.value mustBe MethodNotAllowed(
          createErrorResponseAsJson(
            "METHOD_NOT_ALLOWED",
            "Method Not Allowed"
          )
        )
      }
      "Unknown error response" in {
        val httpResponse = HttpResponse(504, "")

        val result = HttpReader[GetEisRecordsResponse](correlationId).read("GET", "anu-url", httpResponse)

        result.left.value mustBe InternalServerError(
          createErrorResponseAsJson(
            "UNEXPECTED_ERROR",
            "Unexpected Error"
          )
        )
      }
    }
  }

  private def createEisErrorResponseAsJson(errorCode: String, message: String) =
    s"""
       |{
       |  "errorDetail": {
       |    "timestamp": "2023-09-14T11:29:18Z",
       |    "correlationId": "$correlationId",
       |    "errorCode": "$errorCode",
       |    "errorMessage": "$message",
       |    "source": "BACKEND",
       |    "sourceFaultDetail": {
       |      "detail": null
       |    }
       |  }
       |}
        """.stripMargin

  private def createEisErrorResponseWithDetailsAsJson(errorCode: String, message: String, detail: String*) =
    s"""
       |{
       |  "errorDetail": {
       |    "timestamp": "2023-09-14T11:29:18Z",
       |    "correlationId": "$correlationId",
       |    "errorCode": "$errorCode",
       |    "errorMessage": "$message",
       |    "source": "BACKEND",
       |    "sourceFaultDetail": {
       |      "detail": ${Json.toJson(detail)}
       |    }
       |  }
       |}
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
