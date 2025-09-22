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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.response

import play.api.libs.json._
import uk.gov.hmrc.tradergoodsprofilesrouter.models.RemoveNoneFromAssessmentSupport.removeEmptyAssessment
import uk.gov.hmrc.tradergoodsprofilesrouter.models.ResponseModelSupport.removeNulls
import uk.gov.hmrc.tradergoodsprofilesrouter.models.filters.NiphlNumberFilter
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{AccreditationStatus, Assessment}

import java.time.Instant

case class CreateOrUpdateRecordResponse(
  recordId: String,
  eori: String,
  actorId: String,
  traderRef: String,
  comcode: String,
  adviceStatus: AdviceStatus,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Option[Int],
  assessments: Option[Seq[Assessment]],
  supplementaryUnit: Option[BigDecimal],
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant],
  version: Int,
  active: Boolean,
  toReview: Boolean,
  reviewReason: Option[String],
  declarable: String,
  ukimsNumber: Option[String],
  nirmsNumber: Option[String],
  niphlNumber: Option[String],
  createdDateTime: Instant,
  updatedDateTime: Instant
)

object CreateOrUpdateRecordResponse extends NiphlNumberFilter {
  implicit val reads: Reads[CreateOrUpdateRecordResponse] = (json: JsValue) =>
    JsSuccess(
      CreateOrUpdateRecordResponse(
        (json \ "recordId").as[String],
        (json \ "eori").as[String],
        (json \ "actorId").as[String],
        (json \ "traderRef").as[String],
        (json \ "comcode").as[String],
        (json \ "adviceStatus").as[AdviceStatus],
        (json \ "goodsDescription").as[String],
        (json \ "countryOfOrigin").as[String],
        (json \ "category").asOpt[Int],
        (json \ "assessments").asOpt[Seq[Assessment]],
        (json \ "supplementaryUnit").asOpt[BigDecimal],
        (json \ "measurementUnit").asOpt[String],
        (json \ "comcodeEffectiveFromDate").as[Instant],
        (json \ "comcodeEffectiveToDate").asOpt[Instant],
        (json \ "version").as[Int],
        (json \ "active").as[Boolean],
        (json \ "toReview").as[Boolean],
        (json \ "reviewReason").asOpt[String],
        (json \ "declarable").as[String],
        (json \ "ukimsNumber").asOpt[String],
        (json \ "nirmsNumber").asOpt[String],
        (json \ "niphlNumber").asOpt[String],
        (json \ "createdDateTime").as[Instant],
        (json \ "updatedDateTime").as[Instant]
      )
    )

  implicit val writes: Writes[CreateOrUpdateRecordResponse] =
    (createOrUpdateRecordResponse: CreateOrUpdateRecordResponse) =>
      removeNulls(
        Json.obj(
          "recordId"                 -> createOrUpdateRecordResponse.recordId,
          "eori"                     -> createOrUpdateRecordResponse.eori,
          "actorId"                  -> createOrUpdateRecordResponse.actorId,
          "traderRef"                -> createOrUpdateRecordResponse.traderRef,
          "comcode"                  -> createOrUpdateRecordResponse.comcode,
          "adviceStatus"             -> createOrUpdateRecordResponse.adviceStatus,
          "goodsDescription"         -> createOrUpdateRecordResponse.goodsDescription,
          "countryOfOrigin"          -> createOrUpdateRecordResponse.countryOfOrigin,
          "category"                 -> createOrUpdateRecordResponse.category,
          "assessments"              -> removeEmptyAssessment(createOrUpdateRecordResponse.assessments),
          "supplementaryUnit"        -> createOrUpdateRecordResponse.supplementaryUnit,
          "measurementUnit"          -> createOrUpdateRecordResponse.measurementUnit,
          "comcodeEffectiveFromDate" -> createOrUpdateRecordResponse.comcodeEffectiveFromDate,
          "comcodeEffectiveToDate"   -> createOrUpdateRecordResponse.comcodeEffectiveToDate,
          "version"                  -> createOrUpdateRecordResponse.version,
          "active"                   -> createOrUpdateRecordResponse.active,
          "toReview"                 -> createOrUpdateRecordResponse.toReview,
          "reviewReason"             -> createOrUpdateRecordResponse.reviewReason,
          "declarable"               -> createOrUpdateRecordResponse.declarable,
          "ukimsNumber"              -> createOrUpdateRecordResponse.ukimsNumber,
          "nirmsNumber"              -> createOrUpdateRecordResponse.nirmsNumber,
          "niphlNumber"              -> removeLeadingDashes(
            createOrUpdateRecordResponse.niphlNumber
          ),
          "createdDateTime"          -> createOrUpdateRecordResponse.createdDateTime,
          "updatedDateTime"          -> createOrUpdateRecordResponse.updatedDateTime
        )
      )

  def apply(createOrUpdateRecordEisResponse: CreateOrUpdateRecordEisResponse): CreateOrUpdateRecordResponse =
    CreateOrUpdateRecordResponse(
      recordId = createOrUpdateRecordEisResponse.recordId,
      eori = createOrUpdateRecordEisResponse.eori,
      actorId = createOrUpdateRecordEisResponse.actorId,
      traderRef = createOrUpdateRecordEisResponse.traderRef,
      comcode = createOrUpdateRecordEisResponse.comcode,
      adviceStatus = translateAccreditationStatus(createOrUpdateRecordEisResponse.accreditationStatus),
      goodsDescription = createOrUpdateRecordEisResponse.goodsDescription,
      countryOfOrigin = createOrUpdateRecordEisResponse.countryOfOrigin,
      category = createOrUpdateRecordEisResponse.category,
      assessments = createOrUpdateRecordEisResponse.assessments,
      supplementaryUnit = createOrUpdateRecordEisResponse.supplementaryUnit,
      measurementUnit = createOrUpdateRecordEisResponse.measurementUnit,
      comcodeEffectiveFromDate = createOrUpdateRecordEisResponse.comcodeEffectiveFromDate,
      comcodeEffectiveToDate = createOrUpdateRecordEisResponse.comcodeEffectiveToDate,
      version = createOrUpdateRecordEisResponse.version,
      active = createOrUpdateRecordEisResponse.active,
      toReview = createOrUpdateRecordEisResponse.toReview,
      reviewReason = createOrUpdateRecordEisResponse.reviewReason,
      declarable = createOrUpdateRecordEisResponse.declarable,
      ukimsNumber = createOrUpdateRecordEisResponse.ukimsNumber,
      nirmsNumber = createOrUpdateRecordEisResponse.nirmsNumber,
      niphlNumber = createOrUpdateRecordEisResponse.niphlNumber,
      createdDateTime = createOrUpdateRecordEisResponse.createdDateTime,
      updatedDateTime = createOrUpdateRecordEisResponse.updatedDateTime
    )

  private def translateAccreditationStatus(accreditationStatus: AccreditationStatus): AdviceStatus =
    accreditationStatus match {
      case AccreditationStatus.Approved  => AdviceStatus.AdviceProvided
      case AccreditationStatus.Rejected  => AdviceStatus.AdviceNotProvided
      case AccreditationStatus.Withdrawn => AdviceStatus.AdviceRequestWithdrawn
      case _                             => AdviceStatus.withName(accreditationStatus.entryName)
    }

}
