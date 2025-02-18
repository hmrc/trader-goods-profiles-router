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
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.CreateProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.{AuthTestSupport, HawkIntegrationSpec}
import play.api.libs.ws.WSBodyWritables.writeableOf_String

import java.time.Instant

class CreateProfileIntegrationSpec extends HawkIntegrationSpec with AuthTestSupport with BeforeAndAfterEach {

  val eori          = "GB123456789001"
  val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  val dateTime      = "2021-12-17T09:30:47.456Z"
  val timestamp     = "Fri, 17 Dec 2021 09:30:47 Z"

  private val url = fullUrl(s"/customs/traders/goods-profiles/$eoriNumber")

  override def hawkConnectorPath: String = "/tgp/createprofile/v1"

  override def beforeEach(): Unit = {
    reset(authConnector)
    withAuthorizedTrader()
    super.beforeEach()
    when(uuidService.uuid).thenReturn("d677693e-9981-4ee3-8574-654981ebe606")
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2021-12-17T09:30:47.456Z"))
  }

  val headers: Seq[(String, String)] = Seq(
    ("Content-Type", "application/json"),
    ("Accept", "application/vnd.hmrc.1.0+json"),
    ("X-Client-ID", "tss")
  )

  "when trying to create a profile" - {
    "it should return a 200 ok when the request is successful" in {
      stubForEis(OK, Some(createProfileResponse.toString()))

      val response = wsClient
        .url(url)
        .withHttpHeaders(headers: _*)
        .post(createProfileRequest)
        .futureValue

      response.status shouldBe OK
      response.json   shouldBe toJson(createProfileResponse.as[CreateProfileResponse])

      verifyThatDownstreamApiWasCalled(hawkConnectorPath)
    }

    "it should return a 200 ok when the request is successful with optional null fields" in {
      stubForEis(OK, Some(createProfileResponseWithOptionalNullFields.toString()))

      val response = wsClient
        .url(url)
        .withHttpHeaders(headers: _*)
        .post(createProfileRequestWithOptionalNullFields)
        .futureValue

      response.status shouldBe OK
      response.json   shouldBe toJson(createProfileResponseWithoutOptionalNullFields.as[CreateProfileResponse])

      verifyThatDownstreamApiWasCalled(hawkConnectorPath)
    }

    "it should return a 500 internal server error if EIS is unavailable" in {
      stubForEis(
        INTERNAL_SERVER_ERROR,
        Some(eisErrorResponse())
      )

      val response = wsClient
        .url(url)
        .withHttpHeaders(headers: _*)
        .post(createProfileRequest)
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
        .url(url)
        .withHttpHeaders(headers: _*)
        .post(createProfileRequest)
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
          .url(fullUrl(s"/customs/traders/goods-profiles/GB123456789015"))
          .withHttpHeaders(headers: _*)
          .post(createProfileRequest)
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
          .withHttpHeaders(("Content-Type", "application/json"), ("X-Client-ID", "tss"))
          .post(createProfileRequest)
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
      post(urlEqualTo(s"$hawkConnectorPath"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(httpStatus)
            .withBody(responseBody.orNull)
        )
    )

  lazy val createProfileRequest: String =
    """
      |{
      |"actorId":"GB098765432112",
      |"ukimsNumber":"XIUKIM47699357400020231115081800",
      |"nirmsNumber":"RMS-GB-123456",
      |"niphlNumber": "SM12345"
      |}
      |""".stripMargin

  lazy val createProfileRequestWithOptionalNullFields: String =
    """
      |{
      |"actorId":"GB098765432112",
      |"ukimsNumber":"XIUKIM47699357400020231115081800",
      |"nirmsNumber": null,
      |"niphlNumber": null
      |}
      |""".stripMargin

  lazy val createProfileResponse: JsValue =
    Json.parse("""
                 |{
                 |"eori": "GB123456789001",
                 |"actorId":"GB098765432112",
                 |"ukimsNumber":"XIUKIM47699357400020231115081800",
                 |"nirmsNumber":"RMS-GB-123456",
                 |"niphlNumber": "SM12345"
                 |}
                 |""".stripMargin)

  lazy val createProfileResponseWithoutOptionalNullFields: JsValue =
    Json.parse("""
                 |{
                 |"eori": "GB123456789001",
                 |"actorId":"GB098765432112",
                 |"ukimsNumber":"XIUKIM47699357400020231115081800"
                 |}
                 |""".stripMargin)

  lazy val createProfileResponseWithOptionalNullFields: JsValue =
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
