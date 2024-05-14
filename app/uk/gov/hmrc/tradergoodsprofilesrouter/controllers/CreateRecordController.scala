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
import play.api.libs.json.{JsPath, JsValue, Json, JsonValidationError}
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.{ApplicationConstants, HeaderNames}

import scala.concurrent.{ExecutionContext, Future}

class CreateRecordController @Inject() (
  cc: ControllerComponents,
  routerService: RouterService,
  uuidService: UuidService
)(implicit
  executionContext: ExecutionContext
) extends BackendController(cc)
    with Logging {

  def create: Action[JsValue] = Action.async(parse.json) { implicit request =>
    val result = for {
      _ <- validateClientId

      createRecordRequest <- request.body
                               .validate[CreateRecordRequest]
                               .asEither
                               .leftMap { errors =>
                                 logger.warn(
                                   "[CreateRecordController] - Create Record Validation JsError in CreateRecordController.create"
                                 )
                                 BadRequest(
                                   toJson(
                                     ErrorResponse(
                                       uuidService.uuid,
                                       ApplicationConstants.BadRequestCode,
                                       ApplicationConstants.BadRequestMessage,
                                       Some(convertError(errors))
                                     )
                                   )
                                 ): Result
                               }
                               .toEitherT[Future]

      response <- routerService.createRecord(createRecordRequest)
    } yield Created(Json.toJson(response))

    result.merge
  }

  private def validateClientId(implicit request: Request[JsValue]): EitherT[Future, Result, String] =
    EitherT.fromOption(
      request.headers.get(HeaderNames.ClientId),
      BadRequest(
        toJson(
          ErrorResponse(
            uuidService.uuid,
            ApplicationConstants.BadRequestCode,
            ApplicationConstants.MissingHeaderClientId
          )
        )
      )
    )

  private def convertError(
    errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]
  ): Seq[Error] =
    extractSimplePaths(errors)
      .map(key => fieldsToErrorCode.get(key).map(res => Error(ApplicationConstants.InvalidRequestObjectCode, res._2)))
      .toSeq
      .flatten

  private def extractSimplePaths(
    errors: scala.collection.Seq[(JsPath, collection.Seq[JsonValidationError])]
  ): collection.Seq[String] =
    errors
      .map(_._1)
      .map(_.path)
      .map(_.mkString)

  private val fieldsToErrorCode: Map[String, (String, String)] = Map(
    "/eori"                                                       -> ("006", ApplicationConstants.InvalidOrMissingEori),
    "/actorId"                                                    -> ("008", ApplicationConstants.InvalidOrMissingActorId),
    "/traderRef"                                                  -> ("009", ApplicationConstants.InvalidOrMissingTraderRef),
    "/comcode"                                                    -> ("011", ApplicationConstants.InvalidOrMissingComcode),
    "/goodsDescription"                                           -> ("012", ApplicationConstants.InvalidOrMissingGoodsDescription),
    "/countryOfOrigin"                                            -> ("013", ApplicationConstants.InvalidOrMissingCountryOfOrigin),
    "/category"                                                   -> ("014", ApplicationConstants.InvalidOrMissingCategory),
    "/assessments"                                                -> ("015", ApplicationConstants.InvalidOrMissingAssessmentId),
    "/supplementaryUnit"                                          -> ("016", ApplicationConstants.InvalidAssessmentPrimaryCategory),
    "/assessments/primaryCategory/condition/type"                 -> ("017", ApplicationConstants.InvalidAssessmentPrimaryCategoryConditionType),
    "/assessments/primaryCategory/condition/conditionId"          -> ("018", ApplicationConstants.InvalidAssessmentPrimaryCategoryConditionId),
    "/assessments/primaryCategory/condition/conditionDescription" -> ("019", ApplicationConstants.InvalidAssessmentPrimaryCategoryConditionDescription),
    "/assessments/primaryCategory/condition/conditionTraderText"  -> ("020", ApplicationConstants.InvalidAssessmentPrimaryCategoryConditionTraderText),
    "/supplementaryUnit"                                          -> ("021", ApplicationConstants.InvalidOrMissingSupplementaryUnit),
    "/measurementUnit"                                            -> ("022", ApplicationConstants.InvalidOrMissingMeasurementUnit),
    "/comcodeEffectiveFromDate"                                   -> ("023", ApplicationConstants.InvalidOrMissingComcodeEffectiveFromDate),
    "/comcodeEffectiveToDate"                                     -> ("024", ApplicationConstants.InvalidOrMissingComcodeEffectiveToDate)
  )
}
