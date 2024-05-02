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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, Json, OFormat, OWrites, Reads, __}
case class GoodsItemRecords(
  eori: String,
  actorId: String,
  recordId: String,
  traderRef: String,
  comcode: String,
  accreditationRequest: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Int,
  assessments: Option[Seq[Assessment]],
  supplementaryUnit: Option[Int],
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: String,
  comcodeEffectiveToDate: Option[String],
  version: Int,
  active: Boolean,
  toReview: Boolean,
  reviewReason: Option[String],
  declarable: String,
  ukimsNumber: String,
  nirmsNumber: Option[String],
  niphlNumber: Option[String]
//  locked: Boolean
//  srcSystemName: String,
//  createdDateTime: String,
//  updatedDateTime: String
)

object GoodsItemRecords {
  implicit val format: Format[GoodsItemRecords] = Json.format[GoodsItemRecords]

//  implicit val goodsItemRecordsWrites: OWrites[GoodsItemRecords] = (
//    (__ \ "eori").write[String] and
//      (__ \ "actorId").write[String] and
//      (__ \ "recordId").write[String] and
//      (__ \ "traderRef").write[String] and
//      (__ \ "comcode").write[String] and
//      (__ \ "accreditationRequest").write[String] and
//      (__ \ "goodsDescription").write[String] and
//      (__ \ "countryOfOrigin").write[String] and
//      (__ \ "category").write[Int] and
//      (__ \ "assessments").write[Option[Seq[Assessment]]] and
//      (__ \ "supplementaryUnit").write[Option[Int]] and
//      (__ \ "measurementUnit").write[Option[String]] and
//      (__ \ "comcodeEffectiveFromDate").write[String] and
//      (__ \ "comcodeEffectiveToDate").write[Option[String]] and
//      (__ \ "version").write[Int] and
//      (__ \ "active").write[Boolean] and
//      (__ \ "toReview").write[Boolean] and
//      (__ \ "reviewReason").write[Option[String]] and
//      (__ \ "declarable").write[String] and
//      (__ \ "ukimsNumber").write[String] and
//      (__ \ "nirmsNumber").write[Option[String]] and
//      (__ \ "niphlNumber").write[Option[String]]
//    )(unlift(GoodsItemRecords.unapply))
}
