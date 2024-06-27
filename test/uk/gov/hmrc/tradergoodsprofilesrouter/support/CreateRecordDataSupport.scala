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
import uk.gov.hmrc.tradergoodsprofilesrouter.models.CreateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateRecordRequest

trait CreateRecordDataSupport {

  lazy val createOrUpdateRecordSampleJson: JsValue = Json
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

  lazy val createRecordEisPayload: JsValue = Json
    .parse("""
             |{
             |    "eori": "GB123456789012",
             |    "actorId": "GB098765432112",
             |    "traderRef": "BAN001001",
             |    "comcode": "10410100",
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

  val createRecordRequest: CreateRecordRequest = Json
    .parse("""
             |{
             |    "actorId": "GB098765432112",
             |    "traderRef": "BAN001001",
             |    "comcode": "10410100",
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

  val createRecordPayload: CreateRecordPayload = Json
    .parse("""
             |{
             |    "eori": "GB123456789011",
             |    "actorId": "GB098765432112",
             |    "traderRef": "BAN001001",
             |    "comcode": "10410100",
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
    .as[CreateRecordPayload]

}
