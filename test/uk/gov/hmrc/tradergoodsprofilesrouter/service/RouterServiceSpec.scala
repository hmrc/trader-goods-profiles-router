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

package uk.gov.hmrc.tradergoodsprofilesrouter.service

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.http.HeaderCarrier
import play.api.http.Status._
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse, RouterError}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants

import scala.concurrent.{ExecutionContext, Future}

class RouterServiceSpec extends AnyWordSpec with Matchers {

  val eisConnector = new EISConnector {
    override def fetchRecord(eori: String, recordId: String, correlationId: String)(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier
    ): Future[GetEisRecordsResponse] = ???
  }

  val uuidService = new UuidService {
    def generateUuid: String = "1234-5678-9012"
  }

  val routerService = new RouterServiceImpl(eisConnector, uuidService)

  "determine500Error" should {

    "return proper RouterError for ErrorDetail with errorCode 200" in {
      val correlationId = "1234-5678-9012"
      val validJson     =
        """
          |{
          |    "timestamp": "2023-09-14T11:29:18Z",
          |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
          |    "errorCode": "200",
          |    "errorMessage": "Internal Server Error",
          |    "source": "BACKEND",
          |    "sourceFaultDetail": {
          |      "detail": null
          |    }
          |  }
        """.stripMargin

      val expectedError = RouterError(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(
          correlationId,
          ApplicationConstants.INVALID_OR_EMPTY_PAYLOAD_CODE,
          ApplicationConstants.INVALID_OR_EMPTY_PAYLOAD_MESSAGE
        )
      )

      val result = routerService.determine500Error(correlationId, validJson)
      result shouldBe expectedError
    }

    "return proper RouterError for ErrorDetail with errorCode 400" in {
      val correlationId = "1234-5678-9012"
      val validJson     =
        """
          |{
          |    "timestamp": "2023-09-14T11:29:18Z",
          |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
          |    "errorCode": "400",
          |    "errorMessage": "Internal Error Response",
          |    "source": "BACKEND",
          |    "sourceFaultDetail": {
          |      "detail": null
          |    }
          |  }
          |""".stripMargin

      val expectedError = RouterError(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(
          correlationId,
          ApplicationConstants.INTERNAL_ERROR_RESPONSE_CODE,
          ApplicationConstants.INTERNAL_ERROR_RESPONSE_MESSAGE
        )
      )

      val result = routerService.determine500Error(correlationId, validJson)
      result shouldBe expectedError
    }

    "return proper RouterError for ErrorDetail with errorCode 401" in {
      val correlationId = "1234-5678-9012"
      val validJson     =
        """
          |{
          |    "timestamp": "2023-09-14T11:29:18Z",
          |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
          |    "errorCode": "401",
          |    "errorMessage": "Unauthorised",
          |    "source": "BACKEND",
          |    "sourceFaultDetail": {
          |      "detail": null
          |    }
          |  }
          |  """.stripMargin

      val expectedError = RouterError(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(
          correlationId,
          ApplicationConstants.UNAUTHORIZED_CODE,
          ApplicationConstants.UNAUTHORIZED_MESSAGE
        )
      )

      val result = routerService.determine500Error(correlationId, validJson)
      result shouldBe expectedError
    }

    "return proper RouterError for ErrorDetail with errorCode 404" in {
      val correlationId = "1234-5678-9012"
      val validJson     =
        """
          |{
          |    "timestamp": "2023-09-14T11:29:18Z",
          |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
          |    "errorCode": "404",
          |    "errorMessage": "Not Found",
          |    "source": "BACKEND",
          |    "sourceFaultDetail": {
          |      "detail": null
          |    }
          |  }""".stripMargin

      val expectedError = RouterError(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(
          correlationId,
          ApplicationConstants.NOT_FOUND_CODE,
          ApplicationConstants.NOT_FOUND_MESSAGE
        )
      )

      val result = routerService.determine500Error(correlationId, validJson)
      result shouldBe expectedError
    }

    "return proper RouterError for ErrorDetail with errorCode 405" in {
      val correlationId = "1234-5678-9012"
      val validJson     =
        """
          |{
          |    "timestamp": "2023-09-14T11:29:18Z",
          |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
          |    "errorCode": "405",
          |    "errorMessage": "Method Not Allowed",
          |    "source": "BACKEND",
          |    "sourceFaultDetail": {
          |      "detail": null
          |    }
          |  }""".stripMargin

      val expectedError = RouterError(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(
          correlationId,
          ApplicationConstants.METHOD_NOT_ALLOWED_CODE,
          ApplicationConstants.METHOD_NOT_ALLOWED_MESSAGE
        )
      )

      val result = routerService.determine500Error(correlationId, validJson)
      result shouldBe expectedError
    }

    "return proper RouterError for ErrorDetail with errorCode 500" in {
      val correlationId = "1234-5678-9012"
      val validJson     =
        """
          |{
          |    "timestamp": "2023-09-14T11:29:18Z",
          |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
          |    "errorCode": "500",
          |    "errorMessage": "Internal Server Error",
          |    "source": "BACKEND",
          |    "sourceFaultDetail": {
          |      "detail": null
          |    }
          |  }""".stripMargin

      val expectedError = RouterError(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(
          correlationId,
          ApplicationConstants.INTERNAL_SERVER_ERROR_CODE,
          ApplicationConstants.INTERNAL_SERVER_ERROR_MESSAGE
        )
      )

      val result = routerService.determine500Error(correlationId, validJson)
      result shouldBe expectedError
    }

    "return proper RouterError for ErrorDetail with errorCode 502" in {
      val correlationId = "1234-5678-9012"
      val validJson     =
        """
          |{
          |    "timestamp": "2023-09-14T11:29:18Z",
          |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
          |    "errorCode": "502",
          |    "errorMessage": "Bad Gateway",
          |    "source": "BACKEND",
          |    "sourceFaultDetail": {
          |      "detail": null
          |    }
          |  }""".stripMargin

      val expectedError = RouterError(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(
          correlationId,
          ApplicationConstants.BAD_GATEWAY_CODE,
          ApplicationConstants.BAD_GATEWAY_MESSAGE
        )
      )

      val result = routerService.determine500Error(correlationId, validJson)
      result shouldBe expectedError
    }

    "return proper RouterError for ErrorDetail with errorCode 503" in {
      val correlationId = "1234-5678-9012"
      val validJson     =
        """
          |{
          |    "timestamp": "2023-09-14T11:29:18Z",
          |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
          |    "errorCode": "503",
          |    "errorMessage": "Service Unavailable",
          |    "source": "BACKEND",
          |    "sourceFaultDetail": {
          |      "detail": null
          |    }
          |  }""".stripMargin

      val expectedError = RouterError(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(
          correlationId,
          ApplicationConstants.SERVICE_UNAVAILABLE_CODE,
          ApplicationConstants.SERVICE_UNAVAILABLE_MESSAGE
        )
      )

      val result = routerService.determine500Error(correlationId, validJson)
      result shouldBe expectedError
    }

    "return RouterError with unknown error code for unrecognized error codes" in {
      val correlationId    = "1234-5678-9012"
      val unknownErrorJson =
        """
          |{
          |    "timestamp": "2023-09-14T11:29:18Z",
          |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
          |    "errorCode": "001",
          |    "errorMessage": "Service Unavailable",
          |    "source": "BACKEND",
          |    "sourceFaultDetail": {
          |      "detail": null
          |    }
          |  }""".stripMargin

      val expectedError = RouterError(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(
          correlationId,
          ApplicationConstants.UNKNOWN_CODE,
          ApplicationConstants.UNKNOWN_MESSAGE
        )
      )

      val result = routerService.determine500Error(correlationId, unknownErrorJson)
      result should be(expectedError)
    }

    "return RouterError with unexpected error message for invalid JSON" in {
      val correlationId = "1234-5678-9012"
      val invalidJson   = """{ "wrongField": "value" }"""

      val expectedError = RouterError(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(
          correlationId,
          ApplicationConstants.UNEXPECTED_ERROR_CODE,
          ApplicationConstants.UNEXPECTED_ERROR_MESSAGE
        )
      )

      val result = routerService.determine500Error(correlationId, invalidJson)
      result shouldBe expectedError
    }

  }

  "parseFaultDetail" should {

    "return Error with message 006 - Missing or invalid mandatory request parameter EORI given detail string with error: 006 and any message" in {
      val detail = "error: 006, message: Mandatory field comcode was missing from body"

      val expectedError = Error(
        ApplicationConstants.INVALID_REQUEST_PARAMETER_CODE,
        ApplicationConstants.INVALID_OR_MISSING_EORI
      )

      val result = routerService.parseFaultDetail(detail)
      result shouldBe expectedError
    }

    "return Error with message 007 - EORI doesn’t exist in the database given detail string with error: 007 and any message" in {
      val detail = "error: 007, message: Mandatory field comcode was missing from body"

      val expectedError =
        Error(ApplicationConstants.INVALID_REQUEST_PARAMETER_CODE, ApplicationConstants.EORI_DOES_NOT_EXISTS)

      val result = routerService.parseFaultDetail(detail)
      result shouldBe expectedError
    }

    "return Error with message 025 - Invalid request parameter recordId given detail string with error: 025 and any message" in {
      val detail = "error: 025, message: Mandatory field comcode was missing from body"

      val expectedError =
        Error(ApplicationConstants.INVALID_REQUEST_PARAMETER_CODE, ApplicationConstants.INVALID_RECORD_ID)

      val result = routerService.parseFaultDetail(detail)
      result shouldBe expectedError
    }

    "return Error with message 026 - recordId does not exist in the database given detail string with error: 025 and any message" in {
      val detail = "error: 026, message: Mandatory field comcode was missing from body"

      val expectedError =
        Error(ApplicationConstants.INVALID_REQUEST_PARAMETER_CODE, ApplicationConstants.RECORD_ID_DOES_NOT_EXISTS)

      val result = routerService.parseFaultDetail(detail)
      result shouldBe expectedError
    }

    "return Error with message Unexpected Error given detail string with any other error and any message" in {
      val detail = "error: 002, message: Mandatory field comcode was missing from body"

      val expectedError =
        Error(ApplicationConstants.UNEXPECTED_ERROR_CODE, ApplicationConstants.UNEXPECTED_ERROR_MESSAGE)

      val result = routerService.parseFaultDetail(detail)
      result shouldBe expectedError
    }

    "return throw an IllegalArgumentException  given detail string without an error and message" in {
      val detail = "002, Mandatory field comcode was missing from body"

      assertThrows[IllegalArgumentException] {
        routerService.parseFaultDetail(detail)
      }
    }
  }

  "determine400Error" should {

    "return ErrorResponse given a valid error detail string with a sequence of sourceFaultDetail" in {
      val correlationId = "1234-5678-9012"
      val validJson     =
        """
          |{
          |    "timestamp": "2023-09-14T11:29:18Z",
          |    "correlationId": "d677693e-9981-4ee3-8574-654981ebe606",
          |    "errorCode": "200",
          |    "errorMessage": "Internal Server Error",
          |    "source": "BACKEND",
          |    "sourceFaultDetail": {
          |      "detail": [
          |        "error: 006, message: Mandatory field comcode was missing from body",
          |        "error: 007, message: eori doesn't exist in the database"
          |      ]
          |    }
          |  }
        """.stripMargin

      val expectedError = ErrorResponse(
        correlationId,
        ApplicationConstants.BAD_REQUEST_CODE,
        ApplicationConstants.BAD_REQUEST_MESSAGE,
        Some(
          Seq(
            Error("INVALID_REQUEST_PARAMETER", "006 - Missing or invalid mandatory request parameter EORI"),
            Error("INVALID_REQUEST_PARAMETER", "007 - EORI doesn’t exist in the database")
          )
        )
      )

      val result = routerService.determine400Error(correlationId, validJson)
      result shouldBe expectedError
    }

    "return unexpected ErrorResponse given an invalid json string" in {
      val correlationId = "1234-5678-9012"
      val validJson     =
        """
          |{
          |    "invalid": "json"
          |  }
    """.stripMargin

      val expectedError = ErrorResponse(
        correlationId,
        ApplicationConstants.UNEXPECTED_ERROR_CODE,
        ApplicationConstants.UNEXPECTED_ERROR_MESSAGE
      )

      val result = routerService.determine400Error(correlationId, validJson)
      result shouldBe expectedError
    }
  }

}
