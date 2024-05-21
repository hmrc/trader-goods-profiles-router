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

package uk.gov.hmrc.tradergoodsprofilesrouter.controllers

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidateHeaderClientId
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.{CreateRecordRequest, UpdateRecordRequest}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.{HeaderNames, ValidationSupport}

import scala.concurrent.{ExecutionContext, Future}

class UpdateRecordController @Inject() (
  cc: ControllerComponents,
  routerService: RouterService,
  uuidService: UuidService,
  validateHeaderClientId: ValidateHeaderClientId
)(implicit
  executionContext: ExecutionContext
) extends BackendController(cc)
    with Logging {

  def update: Action[JsValue] = Action.async(parse.json) { implicit request =>
    val result = for {
      _ <- validateHeaderClientId.validateClientId(request)

      updateRecordRequest <- validateRequestBody(request)

      response <- routerService.updateRecord(updateRecordRequest)
    } yield Ok(Json.toJson(response))

    result.merge
  }

  private def validateRequestBody(implicit request: Request[JsValue]): EitherT[Future, Result, UpdateRecordRequest] =
    request.body
      .validate[UpdateRecordRequest]
      .asEither
      .leftMap { errors =>
        logger.warn(
          "[UpdateRecordController] - Create Record Validation JsError in UpdateRecordController.create"
        )
        BadRequest(
          toJson(
            ErrorResponse(
              uuidService.uuid,
              BadRequestCode,
              BadRequestMessage,
              Some(ValidationSupport.convertError(errors, fieldsToErrorCode))
            )
          )
        ): Result
      }
      .toEitherT[Future]

  private val fieldsToErrorCode: Map[String, (String, String)] = Map(
    "/eori"                                                       -> (InvalidOrMissingEoriCode, InvalidOrMissingEori),
    "/recordId"                                                   -> (RecordIdDoesNotExistsCode, InvalidRecordId),
    "/actorId"                                                    -> (InvalidOrMissingActorIdCode, InvalidOrMissingActorId),
    "/traderRef"                                                  -> (InvalidOrMissingTraderRefCode, InvalidOrMissingTraderRef),
    "/comcode"                                                    -> (InvalidOrMissingComcodeCode, InvalidOrMissingComcode),
    "/goodsDescription"                                           -> (InvalidOrMissingGoodsDescriptionCode, InvalidOrMissingGoodsDescription),
    "/countryOfOrigin"                                            -> (InvalidOrMissingCountryOfOriginCode, InvalidOrMissingCountryOfOrigin),
    "/category"                                                   -> (InvalidOrMissingCategoryCode, InvalidOrMissingCategory),
    "/assessments"                                                -> (InvalidOrMissingAssessmentIdCode, InvalidOrMissingAssessmentId),
    "/supplementaryUnit"                                          -> (InvalidAssessmentPrimaryCategoryCode, InvalidAssessmentPrimaryCategory),
    "/assessments/primaryCategory/condition/type"                 -> (InvalidAssessmentPrimaryCategoryConditionTypeCode, InvalidAssessmentPrimaryCategoryConditionType),
    "/assessments/primaryCategory/condition/conditionId"          -> (InvalidAssessmentPrimaryCategoryConditionIdCode, InvalidAssessmentPrimaryCategoryConditionId),
    "/assessments/primaryCategory/condition/conditionDescription" -> (InvalidAssessmentPrimaryCategoryConditionDescriptionCode, InvalidAssessmentPrimaryCategoryConditionDescription),
    "/assessments/primaryCategory/condition/conditionTraderText"  -> (InvalidAssessmentPrimaryCategoryConditionTraderTextCode, InvalidAssessmentPrimaryCategoryConditionTraderText),
    "/supplementaryUnit"                                          -> (InvalidOrMissingSupplementaryUnitCode, InvalidOrMissingSupplementaryUnit),
    "/measurementUnit"                                            -> (InvalidOrMissingMeasurementUnitCode, InvalidOrMissingMeasurementUnit),
    "/comcodeEffectiveFromDate"                                   -> (InvalidOrMissingComcodeEffectiveFromDateCode, InvalidOrMissingComcodeEffectiveFromDate),
    "/comcodeEffectiveToDate"                                     -> (InvalidOrMissingComcodeEffectiveToDateCode, InvalidOrMissingComcodeEffectiveToDate)
  )
}
