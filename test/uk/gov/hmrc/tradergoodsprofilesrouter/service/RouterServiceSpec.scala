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

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Forbidden, InternalServerError, MethodNotAllowed, NotFound}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants

import scala.concurrent.{ExecutionContext, Future}

class RouterServiceSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with EitherValues
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val eoriNumber    = "eori"
  private val recordId      = "recordId"
  private val correlationId = "1234-5678-9012"
  private val eisConnector  = mock[EISConnector]
  private val uuidService   = mock[UuidService]

  val routerService = new RouterServiceImpl(eisConnector, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(eisConnector, uuidService)
    when(uuidService.uuid).thenReturn(correlationId)
  }

  "fetchRecord" should {
    "return a record item" in {
      val eisResponse = getEisRecordsResponseData
      when(eisConnector.fetchRecord(any, any)(any, any, any))
        .thenReturn(Future.successful(Right(eisResponse)))

      val result = routerService.fetchRecord(eoriNumber, recordId)

      whenReady(result.value) {
        _.value shouldBe eisResponse.goodsItemRecords.head
      }
    }

    "return an bad request error" when {
      "eori does not exist and comcode is missing" in {
        val eisResponse =
          s"""
             |{
             |    "timestamp": "2023-09-14T11:29:18Z",
             |    "correlationId": "$correlationId",
             |    "errorCode": "400",
             |    "errorMessage": "Internal Server Error",
             |    "source": "BACKEND",
             |    "sourceFaultDetail": {
             |      "detail": [
             |        "error: 006, message: Mandatory field comcode was missing from body",
             |        "error: 007, message: eori does not exist in the database"
             |      ]
             |    }
             |  }
        """.stripMargin
        when(eisConnector.fetchRecord(any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 400)))

        val result = routerService.fetchRecord(eoriNumber, recordId)

        whenReady(result.value) {
          _.left.value shouldBe BadRequest(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.BadRequestCode,
                ApplicationConstants.BadRequestMessage,
                Some(
                  Seq(
                    Error("006", "Mandatory field eori was missing from body"),
                    Error("007", "EORI number does not have a TGP")
                  )
                )
              )
            )
          )
        }
      }
      "Unexpected error response given an invalid json string" in {
        val eisResponse =
          s"""
             | {
             |  "invalid": "json"
             |  }
        """.stripMargin
        when(eisConnector.fetchRecord(any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 400)))

        val result = routerService.fetchRecord(eoriNumber, recordId)

        whenReady(result.value) {
          _.left.value shouldBe BadRequest(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.UnexpectedErrorCode,
                ApplicationConstants.UnexpectedErrorMessage
              )
            )
          )
        }
      }
      "Unexpected error code response" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
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
             |}
        """.stripMargin
        when(eisConnector.fetchRecord(any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 400)))

        val result = routerService.fetchRecord(eoriNumber, recordId)

        whenReady(result.value) {
          _.left.value shouldBe BadRequest(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.BadRequestCode,
                ApplicationConstants.BadRequestMessage,
                Some(
                  Seq(
                    Error(ApplicationConstants.UnexpectedErrorCode, ApplicationConstants.UnexpectedErrorMessage)
                  )
                )
              )
            )
          )
        }
      }
      "Unable to parse source fault detail" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
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
             |}
        """.stripMargin
        when(eisConnector.fetchRecord(any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 400)))

        val exception = intercept[IllegalArgumentException] {
          await(routerService.fetchRecord(eoriNumber, recordId).value)
        }

        exception.getMessage should be(s"Unable to parse fault detail for correlation Id: $correlationId")

      }
    }
    "return an error" when {
      "Forbidden response" in {
        val emptyResponse = ""
        when(eisConnector.fetchRecord(any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(emptyResponse, 403)))

        val result = routerService.fetchRecord(eoriNumber, recordId)

        whenReady(result.value) {
          _.left.value shouldBe Forbidden(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.ForbiddenCode,
                ApplicationConstants.ForbiddenMessage
              )
            )
          )
        }
      }
      "Not found response" in {
        val emptyResponse = ""
        when(eisConnector.fetchRecord(any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(emptyResponse, 404)))

        val result = routerService.fetchRecord(eoriNumber, recordId)

        whenReady(result.value) {
          _.left.value shouldBe NotFound(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.NotFoundCode,
                ApplicationConstants.NotFoundMessage
              )
            )
          )
        }
      }
      "Method not allowed response" in {
        val emptyResponse = ""
        when(eisConnector.fetchRecord(any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(emptyResponse, 405)))

        val result = routerService.fetchRecord(eoriNumber, recordId)

        whenReady(result.value) {
          _.left.value shouldBe MethodNotAllowed(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.MethodNotAllowedCode,
                ApplicationConstants.MethodNotAllowedMessage
              )
            )
          )
        }
      }
      "Unknown error response" in {
        val emptyResponse = ""
        when(eisConnector.fetchRecord(any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(emptyResponse, 504)))

        val result = routerService.fetchRecord(eoriNumber, recordId)

        whenReady(result.value) {
          _.left.value shouldBe InternalServerError(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.UnexpectedErrorCode,
                ApplicationConstants.UnexpectedErrorMessage
              )
            )
          )
        }
      }
    }

  }

  "fetchRecords" should {
    "return a records" in {
      val eisResponse = getEisRecordsResponseData
      when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
        .thenReturn(Future.successful(Right(eisResponse)))

      val result = routerService.fetchRecords(eoriNumber)

      whenReady(result.value) {
        _.value shouldBe eisResponse
      }
    }

    "return an internal server error" when {
      "Invalid payload response" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
             |    "timestamp": "2023-09-14T11:29:18Z",
             |    "correlationId": "$correlationId",
             |    "errorCode": "200",
             |    "errorMessage": "Internal Server Error",
             |    "source": "BACKEND",
             |    "sourceFaultDetail": {
             |      "detail": null
             |    }
             |  }
             |}
        """.stripMargin
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 500)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe InternalServerError(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.InvalidOrEmptyPayloadCode,
                ApplicationConstants.InvalidOrEmptyPayloadMessage
              )
            )
          )
        }
      }
      "Payload schema mismatch" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
             |    "timestamp": "2023-09-14T11:29:18Z",
             |    "correlationId": "$correlationId",
             |    "errorCode": "400",
             |    "errorMessage": "Internal Error Response",
             |    "source": "BACKEND",
             |    "sourceFaultDetail": {
             |      "detail": null
             |    }
             |  }
             |}
        """.stripMargin
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 500)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe InternalServerError(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.InternalErrorResponseCode,
                ApplicationConstants.InternalErrorResponseMessage
              )
            )
          )
        }
      }
      "Unauthorised" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
             |    "timestamp": "2023-09-14T11:29:18Z",
             |    "correlationId": "$correlationId",
             |    "errorCode": "401",
             |    "errorMessage": "Unauthorised",
             |    "source": "BACKEND",
             |    "sourceFaultDetail": {
             |      "detail": null
             |    }
             |  }
             |}
        """.stripMargin
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 500)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe InternalServerError(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.UnauthorizedCode,
                ApplicationConstants.UnauthorizedMessage
              )
            )
          )
        }
      }
      "Not found" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
             |    "timestamp": "2023-09-14T11:29:18Z",
             |    "correlationId": "$correlationId",
             |    "errorCode": "404",
             |    "errorMessage": "Not Found",
             |    "source": "BACKEND",
             |    "sourceFaultDetail": {
             |      "detail": null
             |    }
             |  }
             |}
        """.stripMargin
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 500)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe InternalServerError(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.NotFoundCode,
                ApplicationConstants.NotFoundMessage
              )
            )
          )
        }
      }
      "Method not allowed" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
             |    "timestamp": "2023-09-14T11:29:18Z",
             |    "correlationId": "$correlationId",
             |    "errorCode": "405",
             |    "errorMessage": "Method Not Allowed",
             |    "source": "BACKEND",
             |    "sourceFaultDetail": {
             |      "detail": null
             |    }
             |  }
             |}
        """.stripMargin
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 500)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe InternalServerError(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.MethodNotAllowedCode,
                ApplicationConstants.MethodNotAllowedMessage
              )
            )
          )
        }
      }
      "Internal server error" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
             |    "timestamp": "2023-09-14T11:29:18Z",
             |    "correlationId": "$correlationId",
             |    "errorCode": "500",
             |    "errorMessage": "Internal Server Error",
             |    "source": "BACKEND",
             |    "sourceFaultDetail": {
             |      "detail": null
             |    }
             |  }
             |}
        """.stripMargin
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 500)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe InternalServerError(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.InternalServerErrorCode,
                ApplicationConstants.InternalServerErrorMessage
              )
            )
          )
        }
      }
      "Bad gateway" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
             |    "timestamp": "2023-09-14T11:29:18Z",
             |    "correlationId": "$correlationId",
             |    "errorCode": "502",
             |    "errorMessage": "Bad Gateway",
             |    "source": "BACKEND",
             |    "sourceFaultDetail": {
             |      "detail": null
             |    }
             |  }
             |}
        """.stripMargin
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 500)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe InternalServerError(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.BadGatewayCode,
                ApplicationConstants.BadGatewayMessage
              )
            )
          )
        }
      }
      "Service unavailable" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
             |    "timestamp": "2023-09-14T11:29:18Z",
             |    "correlationId": "$correlationId",
             |    "errorCode": "503",
             |    "errorMessage": "Service Unavailable",
             |    "source": "BACKEND",
             |    "sourceFaultDetail": {
             |      "detail": null
             |    }
             |  }
             |}
        """.stripMargin
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 500)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe InternalServerError(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.ServiceUnavailableCode,
                ApplicationConstants.ServiceUnavailableMessage
              )
            )
          )
        }
      }
      "Unknown error response" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
             |    "timestamp": "2023-09-14T11:29:18Z",
             |    "correlationId": "$correlationId",
             |    "errorCode": "001",
             |    "errorMessage": "Service Unavailable",
             |    "source": "BACKEND",
             |    "sourceFaultDetail": {
             |      "detail": null
             |    }
             |  }
             |}
        """.stripMargin
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 500)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe InternalServerError(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.UnknownCode,
                ApplicationConstants.UnknownMessage
              )
            )
          )
        }
      }
      "Unexpected error is thrown" in {
        val invalidJson = """{ "wrongField": "value" }"""
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(invalidJson, 500)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe InternalServerError(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.UnexpectedErrorCode,
                ApplicationConstants.UnexpectedErrorMessage
              )
            )
          )
        }
      }
    }
    "return an bad request error" when {
      "eori does not exist and comcode is missing" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
             |    "timestamp": "2023-09-14T11:29:18Z",
             |    "correlationId": "$correlationId",
             |    "errorCode": "400",
             |    "errorMessage": "Internal Server Error",
             |    "source": "BACKEND",
             |    "sourceFaultDetail": {
             |      "detail": [
             |        "error: 006, message: Mandatory field comcode was missing from body",
             |        "error: 007, message: eori does not exist in the database"
             |      ]
             |    }
             |  }
             |}
        """.stripMargin
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 400)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe BadRequest(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.BadRequestCode,
                ApplicationConstants.BadRequestMessage,
                Some(
                  Seq(
                    Error("006", "Mandatory field eori was missing from body"),
                    Error("007", "EORI number does not have a TGP")
                  )
                )
              )
            )
          )
        }
      }
      "invalid query parameters lastUpdatedDate, page and size" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
             |    "timestamp": "2023-09-14T11:29:18Z",
             |    "correlationId": "$correlationId",
             |    "errorCode": "400",
             |    "errorMessage": "Internal Server Error",
             |    "source": "BACKEND",
             |    "sourceFaultDetail": {
             |      "detail": [
             |        "error: 028, message: Invalid optional request parameter lastUpdatedDate",
             |        "error: 029, message: Invalid optional request parameter page",
             |        "error: 030, message: Invalid optional request parameter size"
             |      ]
             |    }
             |  }
             |}
        """.stripMargin
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 400)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe BadRequest(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.BadRequestCode,
                ApplicationConstants.BadRequestMessage,
                Some(
                  Seq(
                    Error("028", "The URL parameter lastUpdatedDate is in the wrong format"),
                    Error("029", "The URL parameter page is in the wrong format"),
                    Error("030", "The URL parameter size is in the wrong format")
                  )
                )
              )
            )
          )
        }
      }
      "Unexpected error response given an invalid json string" in {
        val eisResponse =
          s"""
             | {
             |  "invalid": "json"
             |  }
        """.stripMargin
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 400)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe BadRequest(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.UnexpectedErrorCode,
                ApplicationConstants.UnexpectedErrorMessage
              )
            )
          )
        }
      }
      "Unexpected error code response" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
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
             |}
        """.stripMargin
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 400)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe BadRequest(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.BadRequestCode,
                ApplicationConstants.BadRequestMessage,
                Some(
                  Seq(
                    Error(ApplicationConstants.UnexpectedErrorCode, ApplicationConstants.UnexpectedErrorMessage)
                  )
                )
              )
            )
          )
        }
      }
      "Unable to parse source fault detail" in {
        val eisResponse =
          s"""
             |{
             |  "errorDetail": {
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
             |}
        """.stripMargin
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(eisResponse, 400)))

        val exception = intercept[IllegalArgumentException] {
          await(routerService.fetchRecords(eoriNumber).value)
        }

        exception.getMessage should be(s"Unable to parse fault detail for correlation Id: $correlationId")

      }
    }
    "return an error" when {
      "Forbidden response" in {
        val emptyResponse = ""
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(emptyResponse, 403)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe Forbidden(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.ForbiddenCode,
                ApplicationConstants.ForbiddenMessage
              )
            )
          )
        }
      }
      "Not found response" in {
        val emptyResponse = ""
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(emptyResponse, 404)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe NotFound(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.NotFoundCode,
                ApplicationConstants.NotFoundMessage
              )
            )
          )
        }
      }
      "Method not allowed response" in {
        val emptyResponse = ""
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(emptyResponse, 405)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe MethodNotAllowed(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.MethodNotAllowedCode,
                ApplicationConstants.MethodNotAllowedMessage
              )
            )
          )
        }
      }
      "Unknown error response" in {
        val emptyResponse = ""
        when(eisConnector.fetchRecords(any, any, any, any)(any, any, any))
          .thenReturn(Future.failed(UpstreamErrorResponse(emptyResponse, 504)))

        val result = routerService.fetchRecords(eoriNumber)

        whenReady(result.value) {
          _.left.value shouldBe InternalServerError(
            Json.toJson(
              ErrorResponse(
                correlationId,
                ApplicationConstants.UnexpectedErrorCode,
                ApplicationConstants.UnexpectedErrorMessage
              )
            )
          )
        }
      }
    }

  }

  lazy val getEisRecordsResponseData: GetEisRecordsResponse =
    Json
      .parse("""
    |{
    | "goodsItemRecords":
    | [
    |  {
    |    "eori": "GB1234567890",
    |    "actorId": "GB1234567890",
    |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
    |    "traderRef": "BAN001001",
    |    "comcode": "104101000",
    |    "accreditationStatus": "Not requested",
    |    "goodsDescription": "Organic bananas",
    |    "countryOfOrigin": "EC",
    |    "category": 3,
    |    "assessments": [
    |      {
    |        "assessmentId": "abc123",
    |        "primaryCategory": "1",
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
    |    "srcSystemName": "CDAP",
    |    "createdDateTime": "2024-11-18T23:20:19Z",
    |    "updatedDateTime": "2024-11-18T23:20:19Z"
    |  }
    |],
    |"pagination":
    | {
    |   "totalRecords": 1,
    |   "currentPage": 0,
    |   "totalPages": 1,
    |   "nextPage": null,
    |   "prevPage": null
    | }
    |}
    |""".stripMargin)
      .as[GetEisRecordsResponse]

  lazy val createRecordResponseData: CreateRecordResponse =
    Json
      .parse("""
          |{
          |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
          |  "eori": "GB123456789012",
          |  "actorId": "GB098765432112",
          |  "traderRef": "BAN001001",
          |  "comcode": "104101000",
          |  "accreditationStatus": "Not Requested",
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
          |  "niphlNumber": "6 S12345",
          |  "createdDateTime": "2024-11-18T23->20->19Z",
          |  "updatedDateTime": "2024-11-18T23->20->19Z",
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
      .as[CreateRecordResponse]

  lazy val createRecordRequest: CreateRecordRequest = Json
    .parse("""
        |{
        |    "eori": "GB123456789012",
        |    "actorId": "GB098765432112",
        |    "traderRef": "BAN001001",
        |    "comcode": "104101000",
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
        |""".stripMargin)
    .as[CreateRecordRequest]

}
