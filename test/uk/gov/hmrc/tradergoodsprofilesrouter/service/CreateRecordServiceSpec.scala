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
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{BadRequestErrorResponse, CreateRecordConnector, InternalServerErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.CreateRecordDataSupport

import scala.concurrent.{ExecutionContext, Future}

class CreateRecordServiceSpec
    extends PlaySpec
    with CreateRecordDataSupport
    with EitherValues
    with ScalaFutures
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val eoriNumber    = "GB123456789011"
  private val connector     = mock[CreateRecordConnector]
  private val uuidService   = mock[UuidService]
  private val correlationId = "1234-5678-9012"

  val sut = new CreateRecordService(connector, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(connector, uuidService)
    when(uuidService.uuid).thenReturn(correlationId)
  }

  "test" should {
    "create a record item" in {
      val eisResponse = createOrUpdateRecordSampleJson.as[CreateOrUpdateRecordResponse]
      when(connector.createRecord(any, any)(any))
        .thenReturn(Future.successful(Right(eisResponse)))

      val result = await(sut.createRecord(eoriNumber, createRecordRequest))

      result.value mustBe eisResponse
    }

    "return an internal server error" when {
      "EIS return an error" in {
        val badRequestErrorResponse = createEisErrorResponse
        when(connector.createRecord(any, any)(any))
          .thenReturn(Future.successful(Left(badRequestErrorResponse)))

        val result = await(sut.createRecord(eoriNumber, createRecordRequest))

        result.left.value mustBe badRequestErrorResponse
      }

      "error when an exception is thrown" in {
        when(connector.createRecord(any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = await(sut.createRecord(eoriNumber, createRecordRequest))

        result.left.value mustBe InternalServerErrorResponse(
          ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error")
        )
      }
    }
  }

  private def createEisErrorResponse =
    BadRequestErrorResponse(
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
