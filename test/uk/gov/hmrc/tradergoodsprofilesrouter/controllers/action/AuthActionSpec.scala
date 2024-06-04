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

package uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.mvc.{BodyParsers, Request, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsJson, defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService
import uk.gov.hmrc.tradergoodsprofilesrouter.support.AuthTestSupport

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends PlaySpec with AuthTestSupport with BeforeAndAfterEach {

  implicit private val ec: ExecutionContext = ExecutionContext.global

  private val correlationId = UUID.randomUUID().toString
  private val uuidService   = mock[UuidService]

  private val sut = new AuthAction(
    authConnector,
    mock[BodyParsers.Default],
    uuidService,
    stubMessagesControllerComponents()
  )

  def block(request: Request[_]): Future[Result] =
    Future.successful(Results.Ok)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(authConnector, uuidService)

    when(uuidService.uuid).thenReturn(correlationId)
  }
  "authorise" should {
    "authorised" when {
      "enrolment is valid" in {
        withAuthorizedTrader()

        val result = await(sut.apply(eoriNumber).invokeBlock(FakeRequest(), block))

        result.header.status mustBe OK
        verify(authConnector).authorise(eqTo(Enrolment("HMRC-CUS-ORG")), any)(any, any)
      }
    }

    "return an error" when {
      "enrolment is invalid" in {
        withUnauthorizedTrader(InsufficientEnrolments())

        val result = sut.apply(eoriNumber).invokeBlock(FakeRequest(), block)

        status(result) mustBe UNAUTHORIZED
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "UNAUTHORIZED",
          "message"       -> "Downstream error: The details signed in do not have a Trader Goods Profile"
        )
      }

      "cannot retrieve enrolment" in {
        withAuthorization(EmptyRetrieval)

        val result = sut.apply(eoriNumber).invokeBlock(FakeRequest(), block)

        status(result) mustBe UNAUTHORIZED
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "UNAUTHORIZED",
          "message"       -> "Downstream error: The details signed in do not have a Trader Goods Profile"
        )
      }

      "return internal server error when throwing" in {

        withUnauthorizedTrader(new RuntimeException("unauthorised error"))

        val result = sut.apply(eoriNumber).invokeBlock(FakeRequest("GET", "/get"), block)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "INTERNAL_SERVER_ERROR",
          "message"       -> "Downstream error: Internal server error for /get with error: unauthorised error"
        )
      }
    }
  }
}
