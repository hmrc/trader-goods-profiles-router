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

import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.{BadRequest, Forbidden, InternalServerError, MethodNotAllowed, NotFound}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}

class EisHttpErrorHandlerSpec extends PlaySpec {

  private val correlationId: String = "1234-456"
  class TestHarness extends EisHttpErrorHandler

  "handleErrorResponse" should {
    "return an internal server error" when {
      "Invalid payload response with error code 200" in new TestHarness() { handler =>
        val eisResponse: String = createEisErrorResponseAsJson("200", "Internal Server Error")
        val eisHttpResponse     = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result mustBe InternalServerError(
          createErrorResponseAsJson(
            "INVALID_OR_EMPTY_PAYLOAD",
            "Invalid Response Payload or Empty payload"
          )
        )
      }

      "Invalid payload response with error code 201" in new TestHarness() { handler =>
        val eisResponse: String = createEisErrorResponseAsJson("201", "Internal Server Error")
        val eisHttpResponse     = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result mustBe InternalServerError(
          createErrorResponseAsJson(
            "INVALID_OR_EMPTY_PAYLOAD",
            "Invalid Response Payload or Empty payload"
          )
        )
      }

      "Payload schema mismatch" in new TestHarness() { handler =>
        val eisResponse     = createEisErrorResponseAsJson("400", "Internal Error Response")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result mustBe InternalServerError(
          createErrorResponseAsJson(
            "INTERNAL_ERROR_RESPONSE",
            "Internal Error Response"
          )
        )
      }

      "Unauthorised" in new TestHarness() { handler =>
        val eisResponse     = createEisErrorResponseAsJson("401", "Unauthorised")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result mustBe InternalServerError(
          createErrorResponseAsJson(
            "UNAUTHORIZED",
            "Unauthorized"
          )
        )
      }

      "Not found" in new TestHarness { handler =>
        val eisResponse     = createEisErrorResponseAsJson("404", "Not Found")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result mustBe InternalServerError(
          createErrorResponseAsJson(
            "NOT_FOUND",
            "Not Found"
          )
        )
      }

      "Method not allowed" in new TestHarness { handler =>
        val eisResponse     = createEisErrorResponseAsJson("405", "Method Not Allowed")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result mustBe InternalServerError(
          createErrorResponseAsJson(
            "METHOD_NOT_ALLOWED",
            "Method Not Allowed"
          )
        )
      }
      "Internal server error" in new TestHarness { handler =>
        val eisResponse     = createEisErrorResponseAsJson("500", "Internal Server Error")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result mustBe InternalServerError(
          createErrorResponseAsJson("INTERNAL_SERVER_ERROR", "Internal Server Error")
        )

      }
      "Bad gateway" in new TestHarness { handler =>
        val eisResponse     = createEisErrorResponseAsJson("502", "Bad Gateway")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result mustBe InternalServerError(
          createErrorResponseAsJson("BAD_GATEWAY", "Bad Gateway")
        )

      }
      "Service unavailable" in new TestHarness { handler =>
        val eisResponse     = createEisErrorResponseAsJson("503", "Service Unavailable")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result mustBe InternalServerError(
          createErrorResponseAsJson("SERVICE_UNAVAILABLE", "Service Unavailable")
        )
      }
      "Unknown error response" in new TestHarness { handler =>
        val eisResponse     = createEisErrorResponseAsJson("001", "Service Unavailable")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result mustBe InternalServerError(
          createErrorResponseAsJson("UNKNOWN", "Unknown Error")
        )
      }
      "Unexpected error is thrown" in new TestHarness { handler =>
        val invalidJson     = """{ "wrongField": "value" }"""
        val eisHttpResponse = HttpResponse(500, invalidJson)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result mustBe InternalServerError(
          createErrorResponseAsJson("UNEXPECTED_ERROR", "Unexpected Error")
        )
      }
    }

    "return an bad request error" when {
      "invalid request Parameter" in new TestHarness { handler =>
        val eisResponse = createEisErrorResponseWithDetailsAsJson(
          "400",
          "Internal Server Error",
          generateErrorCodes: _*
        )

        val httpResponse = HttpResponse(400, eisResponse, Map.empty)

        val result = handler.handleErrorResponse(httpResponse, correlationId)

        result mustBe BadRequest(
          createInvalidParameterErrorAsJson(
            "BAD_REQUEST",
            "Bad Request",
            Seq(
              "6"  -> "Mandatory field eori was missing from body or is in the wrong format",
              "7"  -> "EORI number does not have a TGP",
              "8"  -> "Mandatory field actorId was missing from body or is in the wrong format",
              "9"  -> "Mandatory field traderRef was missing from body or is in the wrong format",
              "10" -> "Trying to create or update a record with a duplicate traderRef",
              "11" -> "Mandatory field comcode was missing from body or is in the wrong format",
              "12" -> "Mandatory field goodsDescription was missing from body or is in the wrong format",
              "13" -> "Mandatory field countryOfOrigin was missing from body or is in the wrong format",
              "14" -> "Mandatory field category was missing from body or is in the wrong format",
              "15" -> "Optional field assessmentId is in the wrong format",
              "16" -> "Optional field primaryCategory is in the wrong format",
              "17" -> "Optional field type is in the wrong format",
              "18" -> "Optional field conditionId is in the wrong format",
              "19" -> "Optional field conditionDescription is in the wrong format",
              "20" -> "Optional field conditionTraderText is in the wrong format",
              "21" -> "Optional field supplementaryUnit is in the wrong format",
              "22" -> "Optional field measurementUnit is in the wrong format",
              "23" -> "Mandatory field comcodeEffectiveFromDate was missing from body or is in the wrong format",
              "24" -> "Optional field comcodeEffectiveToDate is in the wrong format",
              "25" -> "The recordId has been provided in the wrong format",
              "26" -> "The requested recordId to update doesn’t exist",
              "27" -> "There is an ongoing accreditation request and the record can not be updated",
              "28" -> "The URL parameter lastUpdatedDate is in the wrong format",
              "29" -> "The URL parameter page is in the wrong format",
              "30" -> "The URL parameter size is in the wrong format",
              "31" -> "This record has been removed and cannot be updated"
            ): _*
          )
        )
      }

      "Unexpected error response given an invalid json string" in new TestHarness { handler =>
        val httpResponse = HttpResponse(400, """{"invalid": "json"}""")

        val result = handler.handleErrorResponse(httpResponse, correlationId)

        result mustBe BadRequest(
          createErrorResponseAsJson("UNEXPECTED_ERROR", "Unexpected Error")
        )
      }
      "Unexpected error code response" in new TestHarness { handler =>
        val eisResponse  = createEisErrorResponseWithDetailsAsJson("400", "Bad Request", "error: 100, message: unknown")
        val httpResponse = HttpResponse(400, eisResponse, Map.empty)

        val result = handler.handleErrorResponse(httpResponse, correlationId)

        result mustBe BadRequest(
          createUnexpectedErrorAsJson(
            "BAD_REQUEST",
            "Bad Request",
            Seq("100" -> "Unrecognised error number"): _*
          )
        )
      }

      "Unable to parse source fault detail" in new TestHarness { handler =>
        val eisResponse  = createEisErrorResponseWithDetailsAsJson("400", "Bad Request", "002, unknown")
        val httpResponse = HttpResponse(400, eisResponse, Map.empty)

        val exception = intercept[IllegalArgumentException] {
          handler.handleErrorResponse(httpResponse, correlationId)
        }

        exception.getMessage mustBe s"Unable to parse fault detail for correlation Id: $correlationId"

      }
    }

    "return an error" when {
      "Forbidden response" in new TestHarness { handler =>
        val httpResponse = HttpResponse(403, "")

        val result = handler.handleErrorResponse(httpResponse, correlationId)

        result mustBe Forbidden(
          createErrorResponseAsJson("FORBIDDEN", "Forbidden")
        )

      }
      "Not found response" in new TestHarness { handler =>
        val httpResponse = HttpResponse(404, "")

        val result = handler.handleErrorResponse(httpResponse, correlationId)

        result mustBe NotFound(
          createErrorResponseAsJson("NOT_FOUND", "Not Found")
        )
      }
      "Method not allowed response" in new TestHarness { handler =>
        val httpResponse = HttpResponse(405, "")

        val result = handler.handleErrorResponse(httpResponse, correlationId)

        result mustBe MethodNotAllowed(
          createErrorResponseAsJson(
            "METHOD_NOT_ALLOWED",
            "Method Not Allowed"
          )
        )
      }
      "Unknown error response" in new TestHarness { handler =>
        val httpResponse = HttpResponse(504, "")

        val result = handler.handleErrorResponse(httpResponse, correlationId)

        result mustBe InternalServerError(
          createErrorResponseAsJson(
            "UNEXPECTED_ERROR",
            "Unexpected Error"
          )
        )
      }
    }
  }

  private def createUnexpectedErrorAsJson(
    code: String,
    message: String,
    errors: (String, String)*
  ): JsValue                                                                                               =
    Json.toJson(
      ErrorResponse(
        correlationId,
        code,
        message,
        Some(errors.map(e => Error("UNEXPECTED_ERROR", e._2, e._1.toInt)))
      )
    )
  private def createInvalidParameterErrorAsJson(
    code: String,
    message: String,
    errors: (String, String)*
  ): JsValue                                                                                               =
    Json.toJson(
      ErrorResponse(
        correlationId,
        code,
        message,
        Some(errors.map(e => Error("INVALID_REQUEST_PARAMETER", e._2, e._1.toInt)))
      )
    )
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
  private def createEisErrorResponseAsJson(errorCode: String, message: String)                             =
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

  private def createErrorResponseAsJson(code: String, message: String): JsValue =
    Json.toJson(ErrorResponse(correlationId, code, message))

  private def generateErrorCodes =
    (6 to 31).map { o =>
      val code = s"00$o".takeRight(3)
      s"error: $code, message: whatever"
    }
}