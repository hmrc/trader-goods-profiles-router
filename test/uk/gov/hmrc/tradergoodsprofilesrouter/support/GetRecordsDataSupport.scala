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

package uk.gov.hmrc.tradergoodsprofilesrouter.support

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{GetEisRecordsResponse, GoodsItemRecords}

trait GetRecordsDataSupport {

  def getResponseDataWithAccreditationStatus(status: String = "Not Requested"): GoodsItemRecords = Json
    .parse(s"""
        |  {
        |    "eori": "GB1234567890",
        |    "actorId": "GB1234567890",
        |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
        |    "traderRef": "BAN001001",
        |    "comcode": "10410100",
        |    "accreditationStatus": "$status",
        |    "goodsDescription": "Organic bananas",
        |    "countryOfOrigin": "EC",
        |    "category": 3,
        |    "assessments": [
        |      {
        |        "assessmentId": "abc123",
        |        "primaryCategory": "1",
        |        "condition": {
        |          "type": "abc123",
        |          "conditionId": "Y923",
        |          "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
        |          "conditionTraderText": "Excluded product"
        |        }
        |      }
        |    ],
        |    "supplementaryUnit": 500,
        |    "measurementUnit": "square meters(m^2)",
        |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
        |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
        |    "version": 1,
        |    "active": true,
        |    "toReview": false,
        |    "reviewReason": null,
        |    "declarable": "IMMI declarable",
        |    "ukimsNumber": "XIUKIM47699357400020231115081800",
        |    "nirmsNumber": "RMS-GB-123456",
        |    "niphlNumber": "6 S12345",
        |    "locked": false,
        |    "createdDateTime": "2024-11-18T23:20:19Z",
        |    "updatedDateTime": "2024-11-18T23:20:19Z"
        |  }
        |""".stripMargin)
    .as[GoodsItemRecords]

  def getMultipleRecordResponseData(eori: String = "GB1234567890"): GetEisRecordsResponse = Json
    .parse(s"""
              |{
              |"goodsItemRecords":
              |[
              |  {
              |    "eori": "$eori",
              |    "actorId": "GB1234567890",
              |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
              |    "traderRef": "BAN001001",
              |    "comcode": "10410100",
              |    "accreditationStatus": "Not requested",
              |    "goodsDescription": "Organic bananas",
              |    "countryOfOrigin": "EC",
              |    "category": 3,
              |    "assessments": [
              |      {
              |        "assessmentId": "abc123",
              |        "primaryCategory": 1,
              |        "condition": {
              |          "type": "abc123",
              |          "conditionId": "Y923",
              |          "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
              |          "conditionTraderText": "Excluded product"
              |        }
              |      }
              |    ],
              |    "supplementaryUnit": 500,
              |    "measurementUnit": "square meters(m^2)",
              |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
              |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
              |    "version": 1,
              |    "active": true,
              |    "toReview": false,
              |    "reviewReason": null,
              |    "declarable": "IMMI declarable",
              |    "ukimsNumber": "XIUKIM47699357400020231115081800",
              |    "nirmsNumber": "RMS-GB-123456",
              |    "niphlNumber": "6 S12345",
              |    "locked": false,
              |    "createdDateTime": "2024-11-18T23:20:19Z",
              |    "updatedDateTime": "2024-11-18T23:20:19Z"
              |  },
              |    {
              |    "eori": "GB1234567890",
              |    "actorId": "GB1234567890",
              |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
              |    "traderRef": "BAN001001",
              |    "comcode": "104101000",
              |    "accreditationStatus": "Not requested",
              |    "goodsDescription": "Organic bananas",
              |    "countryOfOrigin": "EC",
              |    "category": 3,
              |    "assessments": [
              |      {
              |        "assessmentId": "abc123",
              |        "primaryCategory": 1,
              |        "condition": {
              |          "type": "abc123",
              |          "conditionId": "Y923",
              |          "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
              |          "conditionTraderText": "Excluded product"
              |        }
              |      }
              |    ],
              |    "supplementaryUnit": 500,
              |    "measurementUnit": "square meters(m^2)",
              |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
              |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
              |    "version": 1,
              |    "active": true,
              |    "toReview": false,
              |    "reviewReason": null,
              |    "declarable": "IMMI declarable",
              |    "ukimsNumber": "XIUKIM47699357400020231115081800",
              |    "nirmsNumber": "RMS-GB-123456",
              |    "niphlNumber": "6 S12345",
              |    "locked": false,
              |    "createdDateTime": "2024-11-18T23:20:19Z",
              |    "updatedDateTime": "2024-11-18T23:20:19Z"
              |  }
              |],
              |"pagination":
              | {
              |   "totalRecords": 2,
              |   "currentPage": 0,
              |   "totalPages": 1,
              |   "nextPage": null,
              |   "prevPage": null
              | }
              |}
              |""".stripMargin)
    .as[GetEisRecordsResponse]

  val getEisRecordsResponseData: JsValue =
    Json.parse("""
                 |{
                 | "goodsItemRecords":
                 | [
                 |  {
                 |    "eori": "GB1234567890",
                 |    "actorId": "GB1234567890",
                 |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
                 |    "traderRef": "BAN001001",
                 |    "comcode": "10410100",
                 |    "accreditationStatus": "Not requested",
                 |    "goodsDescription": "Organic bananas",
                 |    "countryOfOrigin": "EC",
                 |    "category": 3,
                 |    "assessments": [
                 |      {
                 |        "assessmentId": "abc123",
                 |        "primaryCategory": "1",
                 |        "condition": {
                 |          "type": "abc123",
                 |          "conditionId": "Y923",
                 |          "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
                 |          "conditionTraderText": "Excluded product"
                 |        }
                 |      }
                 |    ],
                 |    "supplementaryUnit": 500,
                 |    "measurementUnit": "square meters(m^2)",
                 |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
                 |    "comcodeEffectiveToDate": null,
                 |    "version": 1,
                 |    "active": true,
                 |    "toReview": false,
                 |    "reviewReason": null,
                 |    "declarable": "IMMI declarable",
                 |    "ukimsNumber": "XIUKIM47699357400020231115081800",
                 |    "nirmsNumber": "RMS-GB-123456",
                 |    "niphlNumber": "6 S12345",
                 |    "locked": false,
                 |    "createdDateTime": "2024-11-18T23:20:19Z",
                 |    "updatedDateTime": "2024-11-18T23:20:19Z"
                 |  }
                 |],
                 |"pagination":
                 | {
                 |   "totalRecords": 1,
                 |   "currentPage": 0,
                 |   "totalPages": 1,
                 |   "nextPage": null,
                 |   "prevPage": null
                 | }
                 |}
                 |""".stripMargin)

}
