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
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{EisHttpErrorResponse, MaintainProfileConnector}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.MaintainProfileRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.MaintainProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class MaintainProfileServiceSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with EitherValues
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val eori          = "GB123456789011"
  private val correlationId = "1234-5678-9012"
  private val connector     = mock[MaintainProfileConnector]
  private val uuidService   = mock[UuidService]
  private val appConfig     = mock[AppConfig]

  val service = new MaintainProfileService(connector, uuidService, appConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(connector, uuidService)
    when(uuidService.uuid).thenReturn(correlationId)
    when(appConfig.isDrop1_1_enabled).thenReturn(false)
  }

  "maintain profile" should {
    "successfully maintain a profile" in {
      when(connector.maintainProfile(any, any)(any))
        .thenReturn(Future.successful(Right(maintainProfileResponse)))

      val result = await(service.maintainProfile(eori, maintainProfileRequest))

      result.value shouldBe maintainProfileResponse
    }

    "return an bad request when EIS return a bad request" in {
      val badRequestErrorResponse = createEisErrorResponse

      when(connector.maintainProfile(any, any)(any))
        .thenReturn(Future.successful(Left(badRequestErrorResponse)))

      val result = await(service.maintainProfile(eori, maintainProfileRequest))

      result.left.value shouldBe badRequestErrorResponse
    }

    "return an internal server error when EIS returns one" in {
      when(connector.maintainProfile(any, any)(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(service.maintainProfile(eori, maintainProfileRequest))

      result.left.value shouldBe EisHttpErrorResponse(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error")
      )
    }

    lazy val maintainProfileRequest: MaintainProfileRequest =
      Json
        .parse("""
            |{
            |"eori": "GB123456789012",
            |"actorId":"GB098765432112",
            |"ukimsNumber":"XIUKIM47699357400020231115081800",
            |"nirmsNumber":"RMS-GB-123456",
            |"niphlNumber": "S12345"
            |}
            |""".stripMargin)
        .as[MaintainProfileRequest]

    lazy val maintainProfileResponse: MaintainProfileResponse =
      Json
        .parse("""
            |{
            |"eori": "GB123456789012",
            |"actorId":"GB098765432112",
            |"ukimsNumber":"XIUKIM47699357400020231115081800",
            |"nirmsNumber":"RMS-GB-123456",
            |"niphlNumber": "6S123456"
            |}
            |""".stripMargin)
        .as[MaintainProfileResponse]

  }

  private def createEisErrorResponse =
    EisHttpErrorResponse(
      BAD_REQUEST,
      ErrorResponse(
        correlationId,
        "BAD_REQUEST",
        "BAD_REQUEST",
        Some(
          Seq(
            Error("INTERNAL_ERROR", "internal error 1", 6),
            Error("INTERNAL_ERROR", "internal error 2", 8)
          )
        )
      )
    )
}
