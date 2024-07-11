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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT}
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.{DELETE, contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.WithdrawAdviceConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService
import uk.gov.hmrc.tradergoodsprofilesrouter.support.FakeAuth.FakeSuccessAuthAction

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
  private val invalidWithdrawReasonFormat = Random.alphanumeric.take(4001).mkString
  private val validWithdrawReason         = invalidWithdrawReasonFormat.take(4000)
  private val url                         = Call(DELETE, s"url?withdrawReason=$validWithdrawReason")

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
    when(connector.delete(any, any)(any)).thenReturn(Future.successful(Right(NO_CONTENT)))
  }
  "delete" should {

    "send the request to EIS" in {
      val result = sut.delete(eoriNumber, recordId)(
        FakeRequest(url).withHeaders("X-Client-ID" -> "TSS")
      )

      status(result) mustBe NO_CONTENT
      verify(connector).delete(eqTo(recordId), eqTo(validWithdrawReason))(any)

    }
    "return an error" when {
      "recordId is not valid " in {
        val result = sut.delete(eoriNumber, "invalid-recordId")(
          FakeRequest(url).withHeaders("X-Client-ID" -> "TSS")
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

    "client Id is missing" in {
      val result = sut.delete(eoriNumber, recordId)(FakeRequest(url))

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

    "withdrawReason query parameter is missing" in {
      val result = sut.delete(eoriNumber, recordId)(
        FakeRequest().withHeaders("X-Client-ID" -> "TSS")
      )

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "BAD_REQUEST",
        "message"       -> "Bad Request",
        "errors"        -> Json.arr(
          Json.obj(
            "code"        -> "INVALID_QUERY_PARAMETER",
            "message"     -> "Digital checked that withdraw reason is > 4000",
            "errorNumber" -> 1018
          )
        )
      )
    }

    "withdrawReason query parameter is less the 4000 char" in {

      val urlWithInvalidParam = Call(DELETE, s"url?withdrawReason=$invalidWithdrawReasonFormat")

      val result = sut.delete(eoriNumber, recordId)(
        FakeRequest(urlWithInvalidParam).withHeaders("X-Client-ID" -> "TSS")
      )

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "BAD_REQUEST",
        "message"       -> "Bad Request",
        "errors"        -> Json.arr(
          Json.obj(
            "code"        -> "INVALID_QUERY_PARAMETER",
            "message"     -> "Digital checked that withdraw reason is > 4000",
            "errorNumber" -> 1018
          )
        )
      )
    }

  }
}
