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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.request

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, JsSuccess, Json}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{Assessment, Condition}

import java.time.Instant

class UpdateRecordRequestSpec extends PlaySpec {

  private val time: Instant          = Instant.now()
  private val condition: Condition   =
    Condition(Some("type"), Some("id"), Some("conditionDescription"), Some("conditionTraderText"))
  private val assessment: Assessment =
    Assessment(Some("1234"), Some(1), Some(condition))

  "UpdateRecordRequest" should {

    "serialise" when {
      "all fields present" in {
        Json.toJson(updateRecordRequest) mustBe updateRecordRequestJson

      }

      "supplementary unit present, but measurement unit is not" in {
        Json.toJson(updateRecordRequestSupplementaryNoMeasure) mustBe updateRecordRequestSupplementaryNoMeasureJson
      }

    }

    "deserialize" when {
      "all fields present" in {
        Json.fromJson[UpdateRecordRequest](updateRecordRequestJson) mustBe JsSuccess(updateRecordRequest)
      }

      "supplementary unit present, but measurement unit is not" in {
        Json.fromJson[UpdateRecordRequest](updateRecordRequestSupplementaryNoMeasureJson) mustBe JsSuccess(
          updateRecordRequestSupplementaryNoMeasure
        )
      }

      "measure is null" in {
        Json.fromJson[UpdateRecordRequest](updateRecordRequestMeasureNullJson) mustBe JsSuccess(
          updateRecordRequestSupplementaryNoMeasure
        )
      }

      "measure is empty string" in {
        Json.fromJson[UpdateRecordRequest](updateRecordRequestMeasureEmptyStringJson) mustBe JsSuccess(
          updateRecordRequestSupplementaryNoMeasure
        )
      }

    }

    "validate" must {
      "work" when {
        "all fields present" in {
          updateRecordRequestJson.validate(UpdateRecordRequest.reads) mustBe JsSuccess(updateRecordRequest)
        }

        "supplementary unit present, but measurement unit is not" in {
          updateRecordRequestSupplementaryNoMeasureJson.validate(UpdateRecordRequest.reads) mustBe JsSuccess(
            updateRecordRequestSupplementaryNoMeasure
          )
        }

        "measure is null" in {
          updateRecordRequestMeasureNullJson.validate(UpdateRecordRequest.reads) mustBe JsSuccess(
            updateRecordRequestSupplementaryNoMeasure
          )
        }

        "measure is empty string" in {
          updateRecordRequestMeasureEmptyStringJson.validate(UpdateRecordRequest.reads) mustBe JsSuccess(
            updateRecordRequestSupplementaryNoMeasure
          )
        }
      }
    }

  }

  private lazy val updateRecordRequest: UpdateRecordRequest =
    UpdateRecordRequest(
      "GB098765432112",
      "traderRef",
      "12012000",
      "goodsDesc",
      "GB",
      Some(1),
      Some(Seq(assessment)),
      Some(1.1),
      Some("kg"),
      time,
      Some(time)
    )

  private lazy val updateRecordRequestSupplementaryNoMeasure: UpdateRecordRequest =
    UpdateRecordRequest(
      "GB098765432112",
      "traderRef",
      "12012000",
      "goodsDesc",
      "GB",
      Some(1),
      Some(Seq(assessment)),
      Some(1.1),
      None,
      time,
      Some(time)
    )

  private lazy val updateRecordRequestJson: JsObject = Json.obj(
    "actorId"                  -> "GB098765432112",
    "traderRef"                -> "traderRef",
    "comcode"                  -> "12012000",
    "goodsDescription"         -> "goodsDesc",
    "countryOfOrigin"          -> "GB",
    "category"                 -> 1,
    "assessments"              -> Json.arr(
      Json.obj(
        "assessmentId"    -> "1234",
        "primaryCategory" -> 1,
        "condition"       -> Json.obj(
          "type"                 -> "type",
          "conditionId"          -> "id",
          "conditionDescription" -> "conditionDescription",
          "conditionTraderText"  -> "conditionTraderText"
        )
      )
    ),
    "supplementaryUnit"        -> 1.1,
    "measurementUnit"          -> "kg",
    "comcodeEffectiveFromDate" -> time.toString,
    "comcodeEffectiveToDate"   -> time.toString
  )

  private lazy val updateRecordRequestSupplementaryNoMeasureJson: JsObject = Json.obj(
    "actorId"                  -> "GB098765432112",
    "traderRef"                -> "traderRef",
    "comcode"                  -> "12012000",
    "goodsDescription"         -> "goodsDesc",
    "countryOfOrigin"          -> "GB",
    "category"                 -> 1,
    "assessments"              -> Json.arr(
      Json.obj(
        "assessmentId"    -> "1234",
        "primaryCategory" -> 1,
        "condition"       -> Json.obj(
          "type"                 -> "type",
          "conditionId"          -> "id",
          "conditionDescription" -> "conditionDescription",
          "conditionTraderText"  -> "conditionTraderText"
        )
      )
    ),
    "supplementaryUnit"        -> 1.1,
    "comcodeEffectiveFromDate" -> time.toString,
    "comcodeEffectiveToDate"   -> time.toString
  )

  private lazy val updateRecordRequestMeasureNullJson: JsObject = Json.obj(
    "actorId"                  -> "GB098765432112",
    "traderRef"                -> "traderRef",
    "comcode"                  -> "12012000",
    "goodsDescription"         -> "goodsDesc",
    "countryOfOrigin"          -> "GB",
    "category"                 -> 1,
    "assessments"              -> Json.arr(
      Json.obj(
        "assessmentId"    -> "1234",
        "primaryCategory" -> 1,
        "condition"       -> Json.obj(
          "type"                 -> "type",
          "conditionId"          -> "id",
          "conditionDescription" -> "conditionDescription",
          "conditionTraderText"  -> "conditionTraderText"
        )
      )
    ),
    "supplementaryUnit"        -> 1.1,
    "measurementUnit"          -> null,
    "comcodeEffectiveFromDate" -> time.toString,
    "comcodeEffectiveToDate"   -> time.toString
  )

  private lazy val updateRecordRequestMeasureEmptyStringJson: JsObject = Json.obj(
    "actorId"                  -> "GB098765432112",
    "traderRef"                -> "traderRef",
    "comcode"                  -> "12012000",
    "goodsDescription"         -> "goodsDesc",
    "countryOfOrigin"          -> "GB",
    "category"                 -> 1,
    "assessments"              -> Json.arr(
      Json.obj(
        "assessmentId"    -> "1234",
        "primaryCategory" -> 1,
        "condition"       -> Json.obj(
          "type"                 -> "type",
          "conditionId"          -> "id",
          "conditionDescription" -> "conditionDescription",
          "conditionTraderText"  -> "conditionTraderText"
        )
      )
    ),
    "supplementaryUnit"        -> 1.1,
    "measurementUnit"          -> "",
    "comcodeEffectiveFromDate" -> time.toString,
    "comcodeEffectiveToDate"   -> time.toString
  )

}
