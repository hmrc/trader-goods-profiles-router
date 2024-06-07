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
import org.scalatest.EitherValues
import org.scalatestplus.play.PlaySpec
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.ValidatedQueryParameters
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.Error
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService

import java.util.UUID
import scala.concurrent.ExecutionContext

class ValidationRulesSpec extends PlaySpec with EitherValues {

  val recordId = UUID.randomUUID().toString

  class TestValidationRules extends BackendBaseController with ValidationRules {
    override def uuidService: UuidService = new UuidService

    override implicit def ec: ExecutionContext = ExecutionContext.global

    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
  }

  "validateClientId" should {
    "return a client id if present" in new TestValidationRules() { validator =>
      val result = validator.validateClientId(FakeRequest().withHeaders("X-Client-ID" -> "any-client-id"))

      result.value mustBe "any-client-id"
    }

    "return an error if X-Client-ID is missing" in new TestValidationRules() { validator =>
      val result = validator.validateClientId(FakeRequest())

      result.left.value mustBe Error("INVALID_HEADER", "Missing mandatory header X-Client-ID", 6000)
    }
  }

  "validateRecordId" should {

    "return a recordId if valid" in new TestValidationRules() { validator =>
      val result = validator.validateRecordId(recordId)

      result.value mustBe recordId
    }

    "return an error" when {
      "recordID is an invalid UUID" in new TestValidationRules() { validator =>
        val result = validator.validateRecordId("invalid-uuid")

        result.left.value mustBe Error(
          "INVALID_QUERY_PARAMETER",
          "Query parameter recordId is in the wrong format",
          25
        )
      }

      "recordID is empty" in new TestValidationRules() { validator =>
        val result = validator.validateRecordId("")

        result.left.value mustBe Error(
          "INVALID_QUERY_PARAMETER",
          "Query parameter recordId is in the wrong format",
          25
        )
      }
    }
  }

  "validateQueryParameter" should {
    "return the validated parameters" in new TestValidationRules() { validator =>
      val result = validator.validateQueryParameters("GB124567897897", recordId)

      result.value mustBe ValidatedQueryParameters("GB124567897897", recordId)
    }
    "return an error" when {
      "actorId is invalid" in
        new TestValidationRules() { validator =>
          val result = validator.validateQueryParameters("GB1245678", recordId)

          result.left.value.length mustBe 1
          result.left.value.head mustBe Error(
            "INVALID_QUERY_PARAMETER",
            "Query parameter actorId is in the wrong format",
            8
          )
        }

      "recordId is invalid" in
        new TestValidationRules() { validator =>
          val result = validator.validateQueryParameters("GB124567897897", "invalid-record-id")

          result.left.value.length mustBe 1
          result.left.value.head mustBe Error(
            "INVALID_QUERY_PARAMETER",
            "Query parameter recordId is in the wrong format",
            25
          )
        }

      "recordId and actorId are invalid" in
        new TestValidationRules() { validator =>
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
              "Query parameter recordId is in the wrong format",
              25
            )
          )
        }
    }
  }
}
