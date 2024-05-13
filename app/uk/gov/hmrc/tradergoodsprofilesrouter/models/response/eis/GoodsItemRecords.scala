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

import play.api.libs.json._
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
  niphlNumber: Option[String],
  locked: Boolean,
  srcSystemName: String,
  createdDateTime: String,
  updatedDateTime: String
)

object GoodsItemRecords {

  implicit val goodsItemRecordsReads: Reads[GoodsItemRecords] = (json: JsValue) =>
    JsSuccess(
      GoodsItemRecords(
        (json \ "eori").as[String],
        (json \ "actorId").as[String],
        (json \ "recordId").as[String],
        (json \ "traderRef").as[String],
        (json \ "comcode").as[String],
        (json \ "accreditationRequest").as[String],
        (json \ "goodsDescription").as[String],
        (json \ "countryOfOrigin").as[String],
        (json \ "category").as[Int],
        (json \ "assessments").asOpt[Seq[Assessment]],
        (json \ "supplementaryUnit").asOpt[Int],
        (json \ "measurementUnit").asOpt[String],
        (json \ "comcodeEffectiveFromDate").as[String],
        (json \ "comcodeEffectiveToDate").asOpt[String],
        (json \ "version").as[Int],
        (json \ "active").as[Boolean],
        (json \ "toReview").as[Boolean],
        (json \ "reviewReason").asOpt[String],
        (json \ "declarable").as[String],
        (json \ "ukimsNumber").as[String],
        (json \ "nirmsNumber").asOpt[String],
        (json \ "niphlNumber").asOpt[String],
        (json \ "locked").as[Boolean],
        (json \ "srcSystemName").as[String],
        (json \ "createdDateTime").as[String],
        (json \ "updatedDateTime").as[String]
      )
    )

  implicit val goodsItemRecordsWrites: Writes[GoodsItemRecords] = (goodsItemRecords: GoodsItemRecords) =>
    Json.obj(
      "eori"                     -> goodsItemRecords.eori,
      "actorId"                  -> goodsItemRecords.actorId,
      "recordId"                 -> goodsItemRecords.recordId,
      "traderRef"                -> goodsItemRecords.traderRef,
      "comcode"                  -> goodsItemRecords.comcode,
      "accreditationRequest"     -> goodsItemRecords.accreditationRequest,
      "goodsDescription"         -> goodsItemRecords.goodsDescription,
      "countryOfOrigin"          -> goodsItemRecords.countryOfOrigin,
      "category"                 -> goodsItemRecords.category,
      "assessments"              -> goodsItemRecords.assessments,
      "supplementaryUnit"        -> goodsItemRecords.supplementaryUnit,
      "measurementUnit"          -> goodsItemRecords.measurementUnit,
      "comcodeEffectiveFromDate" -> goodsItemRecords.comcodeEffectiveFromDate,
      "comcodeEffectiveToDate"   -> goodsItemRecords.comcodeEffectiveToDate,
      "version"                  -> goodsItemRecords.version,
      "active"                   -> goodsItemRecords.active,
      "toReview"                 -> goodsItemRecords.toReview,
      "reviewReason"             -> goodsItemRecords.reviewReason,
      "declarable"               -> goodsItemRecords.declarable,
      "ukimsNumber"              -> goodsItemRecords.ukimsNumber,
      "nirmsNumber"              -> goodsItemRecords.nirmsNumber,
      "niphlNumber"              -> goodsItemRecords.niphlNumber,
      "locked"                   -> goodsItemRecords.locked,
      "srcSystemName"            -> goodsItemRecords.srcSystemName,
      "createdDateTime"          -> goodsItemRecords.createdDateTime,
      "updatedDateTime"          -> goodsItemRecords.updatedDateTime
    )
}
