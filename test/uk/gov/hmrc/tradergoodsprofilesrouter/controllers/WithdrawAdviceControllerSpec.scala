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

package uk.gov.hmrc.tradergoodsprofilesrouter.controllers

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{atLeastOnce, reset, verify, when}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.writeableOf_JsValue
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT}
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.{PUT, contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.WithdrawAdviceConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService
import uk.gov.hmrc.tradergoodsprofilesrouter.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class WithdrawAdviceControllerSpec extends PlaySpec with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val eoriNumber                  = "GB123456789001"
  private val recordId                    = UUID.randomUUID().toString
  private val correlationId               = UUID.randomUUID().toString
  private val connector                   = mock[WithdrawAdviceConnector]
  private val uuidService                 = mock[UuidService]
  private val invalidWithdrawReasonFormat = Random.alphanumeric.take(513).mkString
  private val validWithdrawReason         = invalidWithdrawReasonFormat.take(512)
  private val url                         = Call(PUT, s"url")

  val sut = new WithdrawAdviceController(
    new FakeSuccessAuthAction(),
    connector,
    uuidService,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(uuidService, connector)
    when(uuidService.uuid).thenReturn(correlationId)
    when(connector.put(any, any)(any)).thenReturn(Future.successful(Right(NO_CONTENT)))
  }

  def validHeaders: Seq[(String, String)] = Seq(
    HeaderNames.ClientId -> "clientId",
    HeaderNames.Accept   -> "application/vnd.hmrc.1.0+json"
  )

  "delete" should {

    "send the request to EIS" in {
      val request = FakeRequest(url)
        .withBody(Json.obj("withdrawReason" -> validWithdrawReason))
        .withHeaders(validHeaders: _*)

      val result = sut.withdrawAdvice(eoriNumber, recordId)(request)

      status(result) mustBe NO_CONTENT
      verify(connector).put(eqTo(recordId), eqTo(Some(validWithdrawReason)))(any)
    }

    "withdrawReason query parameter is missing" in {
      val result = sut.withdrawAdvice(eoriNumber, recordId)(
        FakeRequest()
          .withBody(Json.obj())
          .withHeaders(validHeaders: _*)
      )

      status(result) mustBe NO_CONTENT
      verify(connector).put(eqTo(recordId), eqTo(None))(any)
    }

    "return an error" when {
      "recordId is not valid " in {
        val result = sut.withdrawAdvice(eoriNumber, "invalid-recordId")(
          FakeRequest(url).withBody(Json.obj()).withHeaders(validHeaders: _*)
        )

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "BAD_REQUEST",
          "message"       -> "Bad Request",
          "errors"        -> Json.arr(
            Json.obj(
              "code"        -> "INVALID_QUERY_PARAMETER",
              "message"     -> "The recordId has been provided in the wrong format",
              "errorNumber" -> 25
            )
          )
        )
      }
    }

    "return 400 Bad request when mandatory request header X-Client-ID" in {
      val result = sut.withdrawAdvice(eoriNumber, recordId)(
        FakeRequest(url)
          .withBody(Json.obj())
          .withHeaders(validHeaders.filterNot { case (name, _) => name.equalsIgnoreCase("X-Client-ID") }: _*)
      )

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "BAD_REQUEST",
        "message"       -> "Bad Request",
        "errors"        -> Json.arr(
          Json.obj(
            "code"        -> "INVALID_HEADER",
            "message"     -> "Missing mandatory header X-Client-ID",
            "errorNumber" -> 6000
          )
        )
      )
    }

    "return 400 Bad request when mandatory request header Accept is missing" in {
      val result = sut.withdrawAdvice(eoriNumber, recordId)(
        FakeRequest(url)
          .withBody(Json.obj())
          .withHeaders(validHeaders.filterNot { case (name, _) => name.equalsIgnoreCase("Accept") }: _*)
      )

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "BAD_REQUEST",
        "message"       -> "Bad Request",
        "errors"        -> Json.arr(
          Json.obj(
            "code"        -> "INVALID_HEADER",
            "message"     -> "Accept was missing from Header or is in the wrong format",
            "errorNumber" -> 4
          )
        )
      )
    }

    "withdrawReason query parameter is more than 512 char" in {
      val result = sut.withdrawAdvice(eoriNumber, recordId)(
        FakeRequest()
          .withBody(Json.obj("withdrawReason" -> invalidWithdrawReasonFormat))
          .withHeaders(validHeaders: _*)
      )

      status(result) mustBe BAD_REQUEST

      contentAsJson(result) mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "BAD_REQUEST",
        "message"       -> "Bad Request",
        "errors"        -> Json.arr(
          Json.obj(
            "code"        -> "INVALID_QUERY_PARAMETER",
            "message"     -> "Digital checked that withdraw reason is > 512",
            "errorNumber" -> 1018
          )
        )
      )
    }

  }
}
