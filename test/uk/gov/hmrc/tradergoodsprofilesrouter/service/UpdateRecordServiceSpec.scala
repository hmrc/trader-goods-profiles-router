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

import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{EisHttpErrorResponse, UpdateRecordConnector}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.PatchRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.payloads.UpdateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{Assessment, Condition}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.{CreateOrUpdateRecordEisResponse, CreateOrUpdateRecordResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.audit.AuditService
import uk.gov.hmrc.tradergoodsprofilesrouter.support.CreateRecordDataSupport

import java.time.Instant
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

  private val correlationId   = "1234-5678-9012"
  private val eoriNumber      = "GB123456789011"
  private val recordId        = "b2fa315b-2d31-4629-90fc-a7b1a5119873"
  private val connector       = mock[UpdateRecordConnector]
  private val uuidService     = mock[UuidService]
  private val auditService    = mock[AuditService]
  private val dateTimeService = mock[DateTimeService]
  private val dateTime        = Instant.parse("2021-12-17T09:30:47.456Z")

  private val sut = new UpdateRecordService(connector, uuidService, auditService, dateTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(connector, uuidService, auditService)
    when(uuidService.uuid).thenReturn(correlationId)
    when(dateTimeService.timestamp).thenReturn(dateTime)
  }

  "updateRecord" should {
    "update a record item" in {
      val eisResponse = createOrUpdateRecordEisResponseData
      when(connector.updateRecord(any, any)(any))
        .thenReturn(Future.successful(Right(eisResponse)))
      when(auditService.emitAuditUpdateRecord(any, any, any, any, any, any)(any)).thenReturn(Future.successful(Done))

      val result = await(sut.patchRecord(eoriNumber, recordId, updateRecordRequest))

      result.value mustBe createOrUpdateRecordResponseData
      verify(connector).updateRecord(eqTo(expectedPayload), eqTo(correlationId))(any)
      verify(auditService)
        .emitAuditUpdateRecord(
          updateRecordPayload,
          dateTime.toString,
          "SUCCEEDED",
          OK,
          None,
          Some(createOrUpdateRecordResponseData)
        )
    }

    "dateTime value should be formatted to yyyy-mm-dd'T'hh:mm:ssZ" in {
      val eisResponse = createOrUpdateRecordEisResponseData
      when(connector.updateRecord(any, any)(any))
        .thenReturn(Future.successful(Right(eisResponse)))
      when(auditService.emitAuditUpdateRecord(any, any, any, any, any, any)(any)).thenReturn(Future.successful(Done))

      val invalidFormattedDate = Instant.parse("2024-11-18T23:20:19.1324564Z")
      val result               =
        await(
          sut.patchRecord(eoriNumber, recordId, updateRecordRequestWIthInvalidFormattedDate(invalidFormattedDate))
        )

      result.value mustBe createOrUpdateRecordResponseData
      verify(connector).updateRecord(eqTo(expectedPayload), eqTo(correlationId))(any)
      verify(auditService)
        .emitAuditUpdateRecord(
          updateRecordPayload,
          dateTime.toString,
          "SUCCEEDED",
          OK,
          None,
          Some(createOrUpdateRecordResponseData)
        )
    }
    "return an internal server error" when {

      "EIS return an error" in {
        val badRequestErrorResponse = createEisErrorResponse
        when(connector.updateRecord(any, any)(any))
          .thenReturn(Future.successful(Left(badRequestErrorResponse)))

        val result = await(sut.patchRecord(eoriNumber, recordId, updateRecordRequest))

        result.left.value mustBe badRequestErrorResponse
        verify(auditService)
          .emitAuditUpdateRecord(
            updateRecordPayload,
            dateTime.toString,
            "BAD_REQUEST",
            BAD_REQUEST,
            Some(Seq("internal error 1", "internal error 2"))
          )
      }

      "error when an exception is thrown" in {
        when(connector.updateRecord(any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = await(sut.patchRecord(eoriNumber, recordId, updateRecordRequest))

        result.left.value mustBe EisHttpErrorResponse(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error")
        )

        verify(auditService)
          .emitAuditUpdateRecord(
            updateRecordPayload,
            dateTime.toString,
            "UNEXPECTED_ERROR",
            INTERNAL_SERVER_ERROR
          )
      }

    }
  }

  private def updateRecordRequestWIthInvalidFormattedDate(dateTime: Instant) =
    PatchRecordRequest(
      actorId = "GB098765432112",
      traderRef = Some("BAN001001"),
      comcode = Some("10410100"),
      goodsDescription = Some("Organic bananas"),
      countryOfOrigin = Some("EC"),
      category = Some(1),
      assessments = Some(
        Seq(
          Assessment(
            Some("abc123"),
            Some(1),
            Some(
              Condition(
                Some("abc123"),
                Some("Y923"),
                Some(
                  "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law"
                ),
                Some("Excluded product")
              )
            )
          )
        )
      ),
      supplementaryUnit = Some(500),
      measurementUnit = Some("Square metre (m2)"),
      comcodeEffectiveFromDate = Some(dateTime),
      comcodeEffectiveToDate = Some(dateTime)
    )

  private def expectedPayload = {

    val condition  = Condition(
      Some("abc123"),
      Some("Y923"),
      Some("Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law"),
      Some("Excluded product")
    )
    val assessment = Assessment(
      Some("abc123"),
      Some(1),
      Some(condition)
    )
    UpdateRecordPayload(
      eoriNumber,
      recordId,
      "GB098765432112",
      Some("BAN001001"),
      Some("10410100"),
      Some("Organic bananas"),
      Some("EC"),
      Some(1),
      Some(Seq(assessment)),
      Some(500),
      Some("Square metre (m2)"),
      Some(Instant.parse("2024-11-18T23:20:19Z")),
      Some(Instant.parse("2024-11-18T23:20:19Z"))
    )
  }

  val updateRecordRequest: PatchRecordRequest = Json
    .parse("""
             |{
             |    "actorId": "GB098765432112",
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
    .as[PatchRecordRequest]

  val updateRecordPayload: UpdateRecordPayload = Json
    .parse("""
             |{
             |    "eori": "GB123456789011",
             |    "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
             |    "actorId": "GB098765432112",
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
    .as[UpdateRecordPayload]

  val createOrUpdateRecordEisResponseData: CreateOrUpdateRecordEisResponse =
    Json
      .parse("""
               |{
               |  "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
               |  "eori": "GB123456789012",
               |  "actorId": "GB098765432112",
               |  "traderRef": "BAN001001",
               |  "comcode": "10410100",
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
               |  "toReview": true,
               |  "reviewReason": "commodity",
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
      .as[CreateOrUpdateRecordEisResponse]

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
               |  "goodsDescription": "Organic bananas",
               |  "countryOfOrigin": "EC",
               |  "category": 1,
               |  "supplementaryUnit": 500,
               |  "measurementUnit": "Square metre (m2)",
               |  "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
               |  "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
               |  "version": 1,
               |  "active": true,
               |  "toReview": true,
               |  "reviewReason": "commodity",
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

  private def createEisErrorResponse =
    EisHttpErrorResponse(
      BAD_REQUEST,
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
