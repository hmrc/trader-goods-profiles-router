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
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.UpdateRecordConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.UpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.CreateRecordDataSupport

import scala.concurrent.{ExecutionContext, Future}

class UpdateRecordServiceSpec
    extends PlaySpec
    with CreateRecordDataSupport
    with ScalaFutures
    with EitherValues
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val correlationId = "1234-5678-9012"
  private val eoriNumber    = "GB123456789011"
  private val connector     = mock[UpdateRecordConnector]
  private val uuidService   = mock[UuidService]

  private val sut = new UpdateRecordService(connector, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(connector, uuidService)
    when(uuidService.uuid).thenReturn(correlationId)
  }

  "updateRecord" should {
    "update a record item" in {
      val eisResponse = createOrUpdateRecordResponseData
      when(connector.updateRecord(any, any)(any))
        .thenReturn(Future.successful(Right(eisResponse)))

      val result = sut.updateRecord(eoriNumber, "recordId", updateRecordRequest)

      whenReady(result.value) {
        _.value mustBe eisResponse
      }
    }

    "return an internal server error" when {

      "EIS return an error" in {
        when(connector.updateRecord(any, any)(any))
          .thenReturn(Future.successful(Left(BadRequest("error"))))

        val result = sut.updateRecord(eoriNumber, "recordId", updateRecordRequest)

        whenReady(result.value) {
          _.left.value mustBe BadRequest("error")
        }
      }

      "error when an exception is thrown" in {
        when(connector.updateRecord(any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = sut.updateRecord(eoriNumber, "recordId", updateRecordRequest)

        whenReady(result.value) {
          _.left.value mustBe InternalServerError(
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

  val updateRecordRequest: UpdateRecordRequest = Json
    .parse("""
             |{
             |    "eori": "GB123456789001",
             |    "actorId": "GB098765432112",
             |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
             |    "traderRef": "BAN001001",
             |    "comcode": "10410100",
             |    "adviceStatus": "Not Requested",
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
    .as[UpdateRecordRequest]

  val createOrUpdateRecordResponseData: CreateOrUpdateRecordResponse =
    Json
      .parse("""
               |{
               |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |  "eori": "GB123456789012",
               |  "actorId": "GB098765432112",
               |  "traderRef": "BAN001001",
               |  "comcode": "10410100",
               |  "adviceStatus": "Not Requested",
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
               |  "createdDateTime": "2024-11-18T23:20:19Z",
               |  "updatedDateTime": "2024-11-18T23:20:19Z",
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
      .as[CreateOrUpdateRecordResponse]
}