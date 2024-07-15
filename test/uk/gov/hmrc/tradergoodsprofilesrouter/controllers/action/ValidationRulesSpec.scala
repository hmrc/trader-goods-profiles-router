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
import org.mockito.MockitoSugar.when
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules._
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.Error
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.Random

class ValidationRulesSpec extends PlaySpec with ScalaFutures with EitherValues with IntegrationPatience {

  private val recordId                 = UUID.randomUUID().toString
  private val correlationId            = UUID.randomUUID().toString
  private val uuidService: UuidService = mock[UuidService]

  class TestValidationRules(override val uuidService: UuidService) extends BackendBaseController with ValidationRules {

    override implicit def ec: ExecutionContext = ExecutionContext.global

    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
  }

  "validateClientId" should {

    when(uuidService.uuid).thenReturn(correlationId)
    "return a client id if present" in new TestValidationRules(uuidService) { validator =>
      val result = validator.validateClientId(FakeRequest().withHeaders("X-Client-ID" -> "any-client-id"))

      result.value mustBe "any-client-id"
    }

    "return an error if X-Client-ID is missing" in new TestValidationRules(uuidService) { validator =>
      val result = validator.validateClientId(FakeRequest())

      result.left.value mustBe BadRequest(
        Json.obj(
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
      )
    }
  }

  "validateRecordId" should {

    "return a recordId if valid" in new TestValidationRules(uuidService) { validator =>
      val result = validator.validateRecordId(recordId)

      result.value mustBe recordId
    }

    "return an error" when {
      "recordID is an invalid UUID" in new TestValidationRules(uuidService) { validator =>
        val result = validator.validateRecordId("invalid-uuid")

        result.left.value mustBe Error(
          "INVALID_QUERY_PARAMETER",
          "The recordId has been provided in the wrong format",
          25
        )
      }

      "recordID is empty" in new TestValidationRules(uuidService) { validator =>
        val result = validator.validateRecordId("")

        result.left.value mustBe Error(
          "INVALID_QUERY_PARAMETER",
          "The recordId has been provided in the wrong format",
          25
        )
      }
    }
  }

  "validateQueryParameter" should {
    "return the validated parameters" in new TestValidationRules(uuidService) { validator =>
      val result = validator.validateQueryParameters("GB124567897897", recordId)

      result.value mustBe ValidatedQueryParameters("GB124567897897", recordId)
    }
    "return an error" when {
      "actorId is invalid" in
        new TestValidationRules(uuidService) { validator =>
          val result = validator.validateQueryParameters("GB1245678", recordId)

          result.left.value.length mustBe 1
          result.left.value.head mustBe Error(
            "INVALID_QUERY_PARAMETER",
            "Query parameter actorId is in the wrong format",
            8
          )
        }

      "recordId is invalid" in
        new TestValidationRules(uuidService) { validator =>
          val result = validator.validateQueryParameters("GB124567897897", "invalid-record-id")

          result.left.value.length mustBe 1
          result.left.value.head mustBe Error(
            "INVALID_QUERY_PARAMETER",
            "The recordId has been provided in the wrong format",
            25
          )
        }

      "recordId and actorId are invalid" in
        new TestValidationRules(uuidService) { validator =>
          val result = validator.validateQueryParameters("GB1245", "invalid-record-id")

          result.left.value.length mustBe 2
          result.left.value mustBe Seq(
            Error(
              "INVALID_QUERY_PARAMETER",
              "Query parameter actorId is in the wrong format",
              8
            ),
            Error(
              "INVALID_QUERY_PARAMETER",
              "The recordId has been provided in the wrong format",
              25
            )
          )
        }
    }
  }

  "validateRequestBody" should {
    "return bad request error if find an error" in new TestValidationRules(uuidService) {
      validator =>
      implicit val request: FakeRequest[JsValue] =
        FakeRequest().withBody[JsValue](Json.parse("""{"surname": "any-name"}"""))

      val result = validator.validateRequestBody[TestClass](Map("/name" -> ("01", "message-error")))

      result.left.value mustBe BadRequest(
        Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "BAD_REQUEST",
          "message"       -> "Bad Request",
          "errors"        -> Json.arr(
            Json.obj(
              "code"        -> "INVALID_REQUEST_PARAMETER",
              "message"     -> "message-error",
              "errorNumber" -> 1
            )
          )
        )
      )
    }

    "return the Deserialised object" in new TestValidationRules(uuidService) {
      validator =>
      implicit val request: FakeRequest[JsValue] =
        FakeRequest().withBody[JsValue](Json.parse("""{"name": "any-name"}"""))

      val result = validator.validateRequestBody[TestClass](Map("/name" -> ("01", "message-error")))

      result.value mustBe TestClass("any-name")
    }

  }

  "validateWithdrawAdviceQueryParam" should {

    "validate all the parameters" in new TestValidationRules(uuidService) {
      validator =>
      val withdrawReason = Random.alphanumeric.take(512).mkString
      val result         = validator.validateWithdrawAdviceQueryParam(recordId)(
        FakeRequest().withBody(Json.obj("withdrawReason" -> withdrawReason))
      )

      result.value mustBe ValidatedWithdrawAdviceQueryParameters(Some(withdrawReason), recordId)
    }

    "return empty string if withdraw reason is empty" in new TestValidationRules(uuidService) {
      validator =>
      val result = validator.validateWithdrawAdviceQueryParam(recordId)(FakeRequest().withBody(Json.obj()))

      result.value mustBe ValidatedWithdrawAdviceQueryParameters(None, recordId)
    }

    "return a list of errors if withdrawreason and recordId are invalid" in new TestValidationRules(uuidService) {
      validator =>
      val invalidWithdrawReason = Random.alphanumeric.take(513).mkString
      val result                = validator.validateWithdrawAdviceQueryParam("recordId")(
        FakeRequest().withBody(Json.obj("withdrawReason" -> invalidWithdrawReason))
      )

      result.left.value.size mustBe 2
      result.left.value mustBe Seq(
        Error("INVALID_QUERY_PARAMETER", "Digital checked that withdraw reason is > 512", 1018),
        Error("INVALID_QUERY_PARAMETER", "The recordId has been provided in the wrong format", 25)
      )
    }
  }

  "isValidCountryCode" should {
    "return true if countryCode is valid" in {
      isValidCountryCode("GB") mustBe true
    }
    "return false if countryCode is not valid" in {
      isValidCountryCode("GB098765112") mustBe false
    }
  }

  "isValidComcode" should {
    "return true for a valid comcode" in {
      isValidComcode("123456") mustBe true
      isValidComcode("12345678") mustBe true
      isValidComcode("1234567890") mustBe true
    }
    "return false if comcode is not valid" in {
      isValidComcode("111") mustBe false
    }
  }

  "isValidActorId" should {
    "return true if actorId is valid" in {
      isValidActorId("GB123456789012") mustBe true
      isValidActorId("GB123456789012456") mustBe true
    }
    "return false if actorId is not valid " in {
      isValidActorId("111") mustBe false
    }
  }
}

case class TestClass(name: String)

object TestClass {
  implicit val format: OFormat[TestClass] = Json.format[TestClass]
}
