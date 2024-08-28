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
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, METHOD_NOT_ALLOWED, NOT_FOUND}
import play.api.libs.json.Json
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

        result mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "INVALID_OR_EMPTY_PAYLOAD", "Invalid Response Payload or Empty payload")
        )
      }

      "Invalid payload response with error code 201" in new TestHarness() { handler =>
        val eisResponse: String = createEisErrorResponseAsJson("201", "Internal Server Error")
        val eisHttpResponse     = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "INVALID_OR_EMPTY_PAYLOAD", "Invalid Response Payload or Empty payload")
        )
      }

      "Payload schema mismatch without error" in new TestHarness() { handler =>
        val eisResponse     = createEisErrorResponseAsJson("400", "Internal Error Response")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "INTERNAL_ERROR_RESPONSE", "Internal Error Response")
        )
      }

      "Payload schema mismatch with errors" in new TestHarness() { handler =>
        val eisResponse     = createEisErrorResponseWithDetailsAsJson(
          "400",
          "Internal Server Error",
          s"error: 031, message: whatever"
        )
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(
            correlationId,
            "INTERNAL_ERROR_RESPONSE",
            "Internal Error Response",
            Some(Seq(Error("INVALID_REQUEST_PARAMETER", "This record has been removed and cannot be updated", 31)))
          )
        )
      }

      "Unauthorised" in new TestHarness() { handler =>
        val eisResponse     = createEisErrorResponseAsJson("401", "Unauthorised")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "UNAUTHORIZED", "Unauthorized")
        )
      }

      "Not found" in new TestHarness { handler =>
        val eisResponse     = createEisErrorResponseAsJson("404", "Not Found")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "NOT_FOUND", "Not Found")
        )
      }

      "Method not allowed" in new TestHarness { handler =>
        val eisResponse     = createEisErrorResponseAsJson("405", "Method Not Allowed")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "METHOD_NOT_ALLOWED", "Method Not Allowed")
        )
      }
      "Internal server error" in new TestHarness { handler =>
        val eisResponse     = createEisErrorResponseAsJson("500", "Internal Server Error")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "INTERNAL_SERVER_ERROR", "Internal Server Error")
        )
      }
      "Bad gateway" in new TestHarness { handler =>
        val eisResponse     = createEisErrorResponseAsJson("502", "Bad Gateway")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "BAD_GATEWAY", "Bad Gateway")
        )
      }
      "Service unavailable" in new TestHarness { handler =>
        val eisResponse     = createEisErrorResponseAsJson("503", "Service Unavailable")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "SERVICE_UNAVAILABLE", "Service Unavailable")
        )
      }
      "Unknown error response" in new TestHarness { handler =>
        val eisResponse     = createEisErrorResponseAsJson("001", "Service Unavailable")
        val eisHttpResponse = HttpResponse(500, eisResponse)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "UNKNOWN", "Unknown Error")
        )
      }
      "Unexpected error is thrown" in new TestHarness { handler =>
        val invalidJson     = """{ "wrongField": "value" }"""
        val eisHttpResponse = HttpResponse(500, invalidJson)

        val result = handler.handleErrorResponse(eisHttpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "UNEXPECTED_ERROR", "Unexpected Error")
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

        result mustBe EisHttpErrorResponse(
          FORBIDDEN,
          ErrorResponse(
            correlationId,
            "BAD_REQUEST",
            "Bad Request",
            Some(
              Seq(
                "6"    -> "Mandatory field eori was missing from body or is in the wrong format",
                "7"    -> "EORI number does not have a TGP",
                "8"    -> "Mandatory field actorId was missing from body or is in the wrong format",
                "9"    -> "Mandatory field traderRef was missing from body or is in the wrong format",
                "10"   -> "Trying to create or update a record with a duplicate traderRef",
                "11"   -> "Mandatory field comcode was missing from body or is in the wrong format",
                "12"   -> "Mandatory field goodsDescription was missing from body or is in the wrong format",
                "13"   -> "Mandatory field countryOfOrigin was missing from body or is in the wrong format",
                "14"   -> "Mandatory field category was missing from body or is in the wrong format",
                "15"   -> "Optional field assessmentId is in the wrong format",
                "16"   -> "Optional field primaryCategory is in the wrong format",
                "17"   -> "Optional field type is in the wrong format",
                "18"   -> "Optional field conditionId is in the wrong format",
                "19"   -> "Optional field conditionDescription is in the wrong format",
                "20"   -> "Optional field conditionTraderText is in the wrong format",
                "21"   -> "Optional field supplementaryUnit is in the wrong format",
                "22"   -> "Optional field measurementUnit is in the wrong format",
                "23"   -> "Mandatory field comcodeEffectiveFromDate was missing from body or is in the wrong format",
                "24"   -> "Optional field comcodeEffectiveToDate is in the wrong format",
                "25"   -> "The recordId has been provided in the wrong format",
                "26"   -> "The requested recordId to update doesn’t exist",
                "27"   -> "There is an ongoing accreditation request and the record can not be updated",
                "28"   -> "The URL parameter lastUpdatedDate is in the wrong format",
                "29"   -> "The URL parameter page is in the wrong format",
                "30"   -> "The URL parameter size is in the wrong format",
                "31"   -> "This record has been removed and cannot be updated",
                "E001" -> "X-Correlation-ID was missing from Header or is in the wrong format",
                "E002" -> "Request Date was missing from Header or is in the wrong format",
                "E003" -> "X-Forwarded-Host was missing from Header or is in the wrong format",
                "E004" -> "Content-Type was missing from Header or is in the wrong format",
                "E005" -> "Accept was missing from Header or is in the wrong format",
                "E006" -> "Mandatory field receiptDate was missing from body",
                "E007" -> "The eori has been provided in the wrong format",
                "E008" -> "Mandatory field RequestorName was missing from body or is in the wrong format",
                "E009" -> "Mandatory field RequestorEmail was missing from body or is in the wrong format",
                "E010" -> "Mandatory field ukimsNumber was missing from body or in the wrong format",
                "E011" -> "Mandatory field goodsItems was missing from body",
                "E012" -> "The recordId has been provided in the wrong format",
                "E013" -> "Mandatory field traderReference was missing from body",
                "E014" -> "Mandatory field goodsDescription was missing from body or is in the wrong format",
                "E015" -> "Mandatory field commodityCode was missing from body"
              ).map { case (code, message) => Error("INVALID_REQUEST_PARAMETER", message, convertCode(code)) }
            )
          )
        )

      }

      "Unexpected error response given an invalid json string" in new TestHarness { handler =>
        val httpResponse = HttpResponse(400, """{"invalid": "json"}""")

        val result = handler.handleErrorResponse(httpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          BAD_REQUEST,
          ErrorResponse(correlationId, "UNEXPECTED_ERROR", "Unexpected Error")
        )
      }
      "Unexpected error code response" in new TestHarness { handler =>
        val eisResponse  = createEisErrorResponseWithDetailsAsJson("400", "Bad Request", "error: 100, message: unknown")
        val httpResponse = HttpResponse(400, eisResponse, Map.empty)

        val result = handler.handleErrorResponse(httpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          BAD_REQUEST,
          ErrorResponse(
            correlationId,
            "BAD_REQUEST",
            "Bad Request",
            Some(Seq(Error("UNEXPECTED_ERROR", "Unrecognised error number", 100)))
          )
        )
      }

      "Unable to parse source fault detail" in new TestHarness { handler =>
        val eisResponse  = createEisErrorResponseWithDetailsAsJson("400", "Bad Request", "002, unknown")
        val httpResponse = HttpResponse(400, eisResponse, Map.empty)

        val result = handler.handleErrorResponse(httpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          BAD_REQUEST,
          ErrorResponse(
            correlationId,
            "BAD_REQUEST",
            "Bad Request"
          )
        )
      }
    }

    "return an error" when {
      "Forbidden response" in new TestHarness { handler =>
        val httpResponse = HttpResponse(403, "")

        val result = handler.handleErrorResponse(httpResponse, correlationId)

        result mustBe EisHttpErrorResponse(FORBIDDEN, ErrorResponse(correlationId, "FORBIDDEN", "Forbidden"))
      }
      "Not found response" in new TestHarness { handler =>
        val httpResponse = HttpResponse(404, "")

        val result = handler.handleErrorResponse(httpResponse, correlationId)

        result mustBe EisHttpErrorResponse(NOT_FOUND, ErrorResponse(correlationId, "NOT_FOUND", "Not Found"))
      }
      "Method not allowed response" in new TestHarness { handler =>
        val httpResponse = HttpResponse(405, "")

        val result = handler.handleErrorResponse(httpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          METHOD_NOT_ALLOWED,
          ErrorResponse(correlationId, "METHOD_NOT_ALLOWED", "Method Not Allowed")
        )
      }
      "Unknown error response" in new TestHarness { handler =>
        val httpResponse = HttpResponse(504, "")

        val result = handler.handleErrorResponse(httpResponse, correlationId)

        result mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "UNEXPECTED_ERROR", "Unexpected Error")
        )
      }
    }
  }

  def convertCode(code: String): Int                                                                               =
    if (code.startsWith("E")) {
      code.drop(1).toInt + 1000
    } else {
      code.toInt
    }
  private def createEisErrorResponseWithDetailsAsJson(errorCode: String, message: String, detail: String*): String =
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
  private def createEisErrorResponseAsJson(errorCode: String, message: String)                                     =
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

  private def generateErrorCodes = {

    val numericCodes      = (6 to 31).map { o =>
      val code = s"00$o".takeRight(3)
      s"error: $code, message: whatever"
    }
    val alphanumericCodes = (1 to 15).map { o =>
      val code = s"E${"%03d".format(o)}"
      s"error: $code, message: whatever"
    }

    numericCodes ++ alphanumericCodes
  }

}
