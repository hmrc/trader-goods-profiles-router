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
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{FORBIDDEN, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.MaintainProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{AuthTestSupport, HawkIntegrationSpec}

import java.time.Instant

class MaintainProfileIntegrationSpec extends HawkIntegrationSpec with AuthTestSupport with BeforeAndAfterEach {

  val eori          = "GB123456789001"
  val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  val dateTime      = "2021-12-17T09:30:47.456Z"
  val timestamp     = "Fri, 17 Dec 2021 09:30:47 Z"

  override def hawkConnectorPath: String = "/tgp/maintainprofile/v1"

  override def beforeEach(): Unit = {
    reset(authConnector)
    withAuthorizedTrader()
    super.beforeEach()
    when(uuidService.uuid).thenReturn("d677693e-9981-4ee3-8574-654981ebe606")
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }

  // TODO: After drop 1.1 remove x-client-id from headers - Ticket: TGP-2014
  val headers: Seq[(String, String)] = if (appConfig.isClientIdHeaderDisabled) {
    Seq(
      ("Content-Type", "application/json"),
      ("Accept", "application/vnd.hmrc.1.0+json")
    )
  } else {
    Seq(
      ("Content-Type", "application/json"),
      ("Accept", "application/vnd.hmrc.1.0+json"),
      ("X-Client-ID", "tss")
    )
  }

  "when trying to maintain a profile" - {
    "it should return a 200 ok when the request is successful" in {
      stubForEis(OK, Some(maintainProfileResponse.toString()))

      val response = wsClient
        .url(fullUrl(s"/traders/$eori"))
        .withHttpHeaders(headers: _*)
        .put(maintainProfileRequest)
        .futureValue

      response.status shouldBe OK
      response.json   shouldBe toJson(maintainProfileResponse.as[MaintainProfileResponse])

      verifyThatDownstreamApiWasCalled(hawkConnectorPath)
    }

    "it should return a 200 ok when the request is successful with optional null fields" in {
      stubForEis(OK, Some(maintainProfileResponseWithOptionalNullFields.toString()))

      val response = wsClient
        .url(fullUrl(s"/traders/$eori"))
        .withHttpHeaders(headers: _*)
        .put(maintainProfileRequestWithOptionalNullFields)
        .futureValue

      response.status shouldBe OK
      response.json   shouldBe toJson(maintainProfileResponseWithoutOptionalNullFields.as[MaintainProfileResponse])

      verifyThatDownstreamApiWasCalled(hawkConnectorPath)
    }

    "it should return a 500 internal server error if EIS is unavailable" in {
      stubForEis(
        INTERNAL_SERVER_ERROR,
        Some(eisErrorResponse())
      )

      val response = wsClient
        .url(fullUrl(s"/traders/$eori"))
        .withHttpHeaders(headers: _*)
        .put(maintainProfileRequest)
        .futureValue

      response.status shouldBe INTERNAL_SERVER_ERROR
      response.json   shouldBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Internal Server Error"
      )

      verifyThatDownstreamApiWasCalled(hawkConnectorPath)
    }

    "it should return a 403 forbidden if the request is valid but EIS reject the request" in {
      stubForEis(FORBIDDEN)

      val response = wsClient
        .url(fullUrl(s"/traders/$eori"))
        .withHttpHeaders(headers: _*)
        .put(maintainProfileRequest)
        .futureValue

      response.status shouldBe FORBIDDEN
      response.json   shouldBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "FORBIDDEN",
        "message"       -> "Forbidden"
      )

      verifyThatDownstreamApiWasCalled(hawkConnectorPath)
    }

    "it should return a 403 forbidden in the following instances" - {
      "EORI number is not authorized" in {

        val response = wsClient
          .url(fullUrl(s"/traders/GB123456789015"))
          .withHttpHeaders(headers: _*)
          .put(maintainProfileRequest)
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
          .url(fullUrl(s"/traders/$eori"))
          .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
          .put(maintainProfileRequest)
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

  private def stubForEis(httpStatus: Int, responseBody: Option[String] = None) =
    stubFor(
      put(urlEqualTo(s"$hawkConnectorPath"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(httpStatus)
            .withBody(responseBody.orNull)
        )
    )

  lazy val maintainProfileRequest: String =
    """
      |{
      |"actorId":"GB098765432112",
      |"ukimsNumber":"XIUKIM47699357400020231115081800",
      |"nirmsNumber":"RMS-GB-123456",
      |"niphlNumber": "SM12345"
      |}
      |""".stripMargin

  lazy val maintainProfileRequestWithOptionalNullFields: String =
    """
      |{
      |"actorId":"GB098765432112",
      |"ukimsNumber":"XIUKIM47699357400020231115081800",
      |"nirmsNumber": null,
      |"niphlNumber": null
      |}
      |""".stripMargin

  lazy val maintainProfileResponse: JsValue =
    Json.parse("""
                 |{
                 |"eori": "GB123456789012",
                 |"actorId":"GB098765432112",
                 |"ukimsNumber":"XIUKIM47699357400020231115081800",
                 |"nirmsNumber":"RMS-GB-123456",
                 |"niphlNumber": "--1234"
                 |}
                 |""".stripMargin)

  lazy val maintainProfileResponseWithoutOptionalNullFields: JsValue =
    Json.parse("""
                 |{
                 |"eori": "GB123456789012",
                 |"actorId":"GB098765432112"
                 |}
                 |""".stripMargin)

  lazy val maintainProfileResponseWithOptionalNullFields: JsValue =
    Json.parse("""
                 |{
                 |"eori": "GB123456789012",
                 |"actorId":"GB098765432112",
                 |"ukimsNumber": null,
                 |"nirmsNumber": null,
                 |"niphlNumber": null
                 |}
                 |""".stripMargin)

  private def eisErrorResponse(): String =
    Json
      .parse(
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
           |""".stripMargin
      )
      .toString()
}
