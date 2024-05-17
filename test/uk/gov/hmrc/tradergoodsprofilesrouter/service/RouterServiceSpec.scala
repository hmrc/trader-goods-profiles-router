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
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport

import scala.concurrent.{ExecutionContext, Future}

class RouterServiceSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with GetRecordsDataSupport
    with EitherValues
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext  = ExecutionContext.global
  implicit val hc: HeaderCarrier     = HeaderCarrier()
  implicit val correlationId: String = "1234-5678-9012"

  private val eoriNumber   = "eori"
  private val recordId     = "recordId"
  private val eisConnector = mock[EISConnector]
  private val uuidService  = mock[UuidService]

  val routerService = new RouterServiceImpl(eisConnector, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(eisConnector, uuidService)
    when(uuidService.uuid).thenReturn(correlationId)
  }

  "fetchRecord" should {
    "return a record item" in {
      when(eisConnector.fetchRecord(any, any)(any, any))
        .thenReturn(Future.successful(Right(getEisRecordsResponseData.as[GetEisRecordsResponse])))

      val result = routerService.fetchRecord(eoriNumber, recordId)

      whenReady(result.value) {
        _.value shouldBe getEisRecordsResponseData.as[GetEisRecordsResponse].goodsItemRecords.head
      }
    }

    "return an error" when {
      "EIS return an error" in {
        when(eisConnector.fetchRecord(any, any)(any, any))
          .thenReturn(Future.successful(Left(BadRequest("error"))))

        val result = routerService.fetchRecord(eoriNumber, recordId)

        whenReady(result.value) {
          _.left.value shouldBe BadRequest("error")
        }
      }

      "error when an exception is thrown" in {
        when(eisConnector.fetchRecord(any, any)(any, any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = routerService.fetchRecord(eoriNumber, recordId)

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

    val lastUpdateDate = "2024-04-18T23:20:19Z"

    "return a records" in {
      val eisResponse = getEisRecordsResponseData.as[GetEisRecordsResponse]
      when(eisConnector.fetchRecords(any, any, any, any)(any, any))
        .thenReturn(Future.successful(Right(eisResponse)))

      val result = routerService.fetchRecords(eoriNumber, Some(lastUpdateDate), Some(1), Some(1))

      whenReady(result.value) {
        _.value shouldBe eisResponse
      }

      withClue("should call the eisConnector with teh right parameters") {
        verify(eisConnector)
          .fetchRecords(eqTo(eoriNumber), eqTo(Some(lastUpdateDate)), eqTo(Some(1)), eqTo(Some(1)))(any, any)
      }
    }

    "return an error" when {

      "EIS return an error" in {
        when(eisConnector.fetchRecords(any, any, any, any)(any, any))
          .thenReturn(Future.successful(Left(BadRequest("error"))))

        val result = routerService.fetchRecords(eoriNumber, Some(lastUpdateDate), Some(1), Some(1))

        whenReady(result.value) {
          _.left.value shouldBe BadRequest("error")
        }
      }

      "error when an exception is thrown" in {
        when(eisConnector.fetchRecords(any, any, any, any)(any, any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = routerService.fetchRecords(eoriNumber, Some(lastUpdateDate), Some(1), Some(1))

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

  lazy val createRecordResponseData: CreateRecordResponse =
    Json
      .parse("""
          |{
          |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
          |  "eori": "GB123456789012",
          |  "actorId": "GB098765432112",
          |  "traderRef": "BAN001001",
          |  "comcode": "104101000",
          |  "accreditationStatus": "Not Requested",
          |  "goodsDescription": "Organic bananas",
          |  "countryOfOrigin": "EC",
          |  "category": 1,
          |  "supplementaryUnit": 500,
          |  "measurementUnit": "Square metre (m2)",
          |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
          |  "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
          |  "version": 1,
          |  "active": true,
          |  "toReview": false,
          |  "reviewReason": "Commodity code change",
          |  "declarable": "SPIMM",
          |  "ukimsNumber": "XIUKIM47699357400020231115081800",
          |  "nirmsNumber": "RMS-GB-123456",
          |  "niphlNumber": "6 S12345",
          |  "createdDateTime": "2024-11-18T23->20->19Z",
          |  "updatedDateTime": "2024-11-18T23->20->19Z",
          |  "assessments": [
          |    {
          |      "assessmentId": "abc123",
          |      "primaryCategory": 1,
          |      "condition": {
          |        "type": "abc123",
          |        "conditionId": "Y923",
          |        "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
          |        "conditionTraderText": "Excluded product"
          |      }
          |    }
          |  ]
          |}
          |""".stripMargin)
      .as[CreateRecordResponse]

  lazy val createRecordRequest: CreateRecordRequest = Json
    .parse("""
        |{
        |    "eori": "GB123456789012",
        |    "actorId": "GB098765432112",
        |    "traderRef": "BAN001001",
        |    "comcode": "104101000",
        |    "goodsDescription": "Organic bananas",
        |    "countryOfOrigin": "EC",
        |    "category": 1,
        |    "assessments": [
        |        {
        |            "assessmentId": "abc123",
        |            "primaryCategory": 1,
        |            "condition": {
        |                "type": "abc123",
        |                "conditionId": "Y923",
        |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
        |                "conditionTraderText": "Excluded product"
        |            }
        |        }
        |    ],
        |    "supplementaryUnit": 500,
        |    "measurementUnit": "Square metre (m2)",
        |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
        |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
        |}
        |""".stripMargin)
    .as[CreateRecordRequest]

}
