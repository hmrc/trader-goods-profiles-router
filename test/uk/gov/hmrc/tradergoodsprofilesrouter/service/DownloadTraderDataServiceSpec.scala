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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{atLeastOnce, reset, verify, when}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.writeableOf_JsValue
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{DownloadTraderDataConnector, EisHttpErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CorrelationId
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.CreateRecordDataSupport

import scala.concurrent.{ExecutionContext, Future}

class DownloadTraderDataServiceSpec
    extends PlaySpec
    with CreateRecordDataSupport
    with EitherValues
    with ScalaFutures
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val eori          = "GB123456789011"
  private val connector     = mock[DownloadTraderDataConnector]
  private val uuidService   = mock[UuidService]
  private val correlationId = "1234-5678-9012"

  private val service = new DownloadTraderDataService(connector, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(connector, uuidService)
    when(uuidService.uuid).thenReturn(correlationId)
  }

  "download trader data" should {
    "return correlationId when the request is accepted by EIS" in {
      when(connector.requestDownload(eori, correlationId)).thenReturn(Future.successful(Right(ACCEPTED)))

      val result = await(service.requestDownload(eori))

      result.value mustBe CorrelationId(correlationId)
    }

    "return any error that is returned by EIS" in {
      val errorResponse = EisHttpErrorResponse(
        BAD_REQUEST,
        ErrorResponse(
          correlationId,
          "BAD_REQUEST",
          "BAD_REQUEST"
        )
      )

      when(connector.requestDownload(eori, correlationId)).thenReturn(Future.successful(Left(errorResponse)))

      val result = await(service.requestDownload(eori))

      result.left.value mustBe errorResponse
    }

    "returns an internal server error when an exception is thrown by EIS" in {
      when(connector.requestDownload(eori, correlationId))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(service.requestDownload(eori))

      result.left.value mustBe EisHttpErrorResponse(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error")
      )
    }
  }
}
