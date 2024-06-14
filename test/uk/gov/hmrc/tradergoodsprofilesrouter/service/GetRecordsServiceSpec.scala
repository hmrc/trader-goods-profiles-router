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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.GetRecordsConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class GetRecordsServiceSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with GetRecordsDataSupport
    with EitherValues
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val eoriNumber          = "GB123456789011"
  private val recordId            = "recordId"
  private val correlationId       = "1234-5678-9012"
  private val getRecordsConnector = mock[GetRecordsConnector]
  private val uuidService         = mock[UuidService]

  val sut = new GetRecordsService(getRecordsConnector, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(getRecordsConnector, uuidService)
    when(uuidService.uuid).thenReturn(correlationId)
  }

  "fetchRecord" should {
    "return a record item" in {
      when(getRecordsConnector.fetchRecord(any, any, any)(any))
        .thenReturn(Future.successful(Right(getEisRecordsResponseData.as[GetEisRecordsResponse])))

      val result = sut.fetchRecord(eoriNumber, recordId)

      whenReady(result.value) {
        _.value shouldBe getEisRecordsResponseData.as[GetEisRecordsResponse].goodsItemRecords.head
      }
    }

    "return an error" when {
      "EIS return an error" in {
        when(getRecordsConnector.fetchRecord(any, any, any)(any))
          .thenReturn(Future.successful(Left(BadRequest("error"))))

        val result = sut.fetchRecord(eoriNumber, recordId)

        whenReady(result.value) {
          _.left.value shouldBe BadRequest("error")
        }
      }

      "error when an exception is thrown" in {
        when(getRecordsConnector.fetchRecord(any, any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = sut.fetchRecord(eoriNumber, recordId)

        whenReady(result.value) {
          _.left.value shouldBe InternalServerError(
            Json.obj(
              "correlationId" -> correlationId,
              "code"          -> "UNEXPECTED_ERROR",
              "message"       -> "error"
            )
          )
        }
      }

    }

  }

  "fetchRecords" should {

    val lastUpdateDate = Instant.parse("2024-04-18T23:20:19Z")

    "return a records" in {
      val eisResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]
      when(getRecordsConnector.fetchRecords(any, any, any, any, any)(any))
        .thenReturn(Future.successful(Right(eisResponse)))

      val result = sut.fetchRecords(eoriNumber, Some(lastUpdateDate), Some(1), Some(1))

      whenReady(result.value) {
        _.value shouldBe eisResponse
      }

      withClue("should call the getRecordsConnector with teh right parameters") {
        verify(getRecordsConnector)
          .fetchRecords(
            eqTo(eoriNumber),
            eqTo(correlationId),
            eqTo(Some(lastUpdateDate)),
            eqTo(Some(1)),
            eqTo(Some(1))
          )(any)
      }
    }

    "return an error" when {

      "EIS return an error" in {
        when(getRecordsConnector.fetchRecords(any, any, any, any, any)(any))
          .thenReturn(Future.successful(Left(BadRequest("error"))))

        val result = sut.fetchRecords(eoriNumber, Some(lastUpdateDate), Some(1), Some(1))

        whenReady(result.value) {
          _.left.value shouldBe BadRequest("error")
        }
      }

      "error when an exception is thrown" in {
        when(getRecordsConnector.fetchRecords(any, any, any, any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = sut.fetchRecords(eoriNumber, Some(lastUpdateDate), Some(1), Some(1))

        whenReady(result.value) {
          _.left.value shouldBe InternalServerError(
            Json.obj(
              "correlationId" -> correlationId,
              "code"          -> "UNEXPECTED_ERROR",
              "message"       -> "error"
            )
          )
        }
      }

    }
  }

}
