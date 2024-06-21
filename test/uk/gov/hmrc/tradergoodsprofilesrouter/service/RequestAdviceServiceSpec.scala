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
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.CREATED
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{BadRequestErrorResponse, InternalServerErrorResponse, RequestAdviceConnector}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.RequestAdvice
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport

import scala.concurrent.{ExecutionContext, Future}

class RequestAdviceServiceSpec
    extends PlaySpec
    with EitherValues
    with ScalaFutures
    with IntegrationPatience
    with GetRecordsDataSupport
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val eori          = "GB123456789011"
  private val recordId      = "12345"
  private val correlationId = "1234-5678-9012"

  private val connector        = mock[RequestAdviceConnector]
  private val getRecordService = mock[GetRecordsService]
  private val uuidService      = mock[UuidService]

  private val request = new RequestAdvice("Judi Dench", "judi@example.com")

  val service = new RequestAdviceService(connector, getRecordService, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(connector, uuidService, getRecordService)
    when(uuidService.uuid).thenReturn(correlationId)
  }

  "should successfully send a request advice request to EIS" in {

    when(getRecordService.fetchRecord(any, any)(any))
      .thenReturn(Future.successful(Right(getSingleRecordResponseData)))

    when(connector.requestAdvice(any, any)(any))
      .thenReturn(Future.successful(Right(CREATED)))

    val result = await(service.requestAdvice(eori, recordId, request))

    result.value shouldBe CREATED
  }

  "should throw an error if fetch record fails" in {}

  "return an error" when {
    "connector return an error" in {
      val badRequestErrorResponse = createEisErrorResponse
      when(getRecordService.fetchRecord(any, any)(any))
        .thenReturn(Future.successful(Right(getSingleRecordResponseData)))

      when(connector.requestAdvice(any, any)(any))
        .thenReturn(Future.successful(Left(badRequestErrorResponse)))

      val result = await(service.requestAdvice(eori, recordId, request))

      result.left.value mustBe badRequestErrorResponse
    }

    "connector throws a run time exception" in {
      when(getRecordService.fetchRecord(any, any)(any))
        .thenReturn(Future.successful(Right(getSingleRecordResponseData)))

      when(connector.requestAdvice(any, any)(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(service.requestAdvice(eori, recordId, request))

      result.left.value mustBe InternalServerErrorResponse(
        ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error")
      )
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
