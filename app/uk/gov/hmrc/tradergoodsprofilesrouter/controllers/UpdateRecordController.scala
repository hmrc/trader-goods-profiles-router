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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.{BadRequestErrorResponse, fieldsToErrorCode, optionalFieldsToErrorCode}
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.{AuthAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.{PatchRecordRequest, UpdateRecordRequest}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{UpdateRecordService, UuidService}

import scala.concurrent.{ExecutionContext, Future}

class UpdateRecordController @Inject() (
  authAction: AuthAction,
  override val controllerComponents: ControllerComponents,
  updateRecordService: UpdateRecordService,
  appConfig: AppConfig,
  override val uuidService: UuidService
)(implicit override val ec: ExecutionContext)
    extends BackendBaseController
    with ValidationRules
    with Logging {

  def patch(eori: String, recordId: String): Action[JsValue] = authAction(eori).async(parse.json) { implicit request =>
    val result = for {
      _                   <- EitherT.fromEither[Future](validateClientIdIfSupported)
      _                   <- EitherT.fromEither[Future](validateAcceptHeader)
      _                   <- EitherT
                               .fromEither[Future](validateRecordId(recordId))
                               .leftMap(e => BadRequestErrorResponse(uuidService.uuid, Seq(e)).asPresentation)
      updateRecordRequest <-
        EitherT.fromEither[Future](validateRequestBody[PatchRecordRequest](optionalFieldsToErrorCode))
      response            <- patchRecord(eori, recordId, updateRecordRequest)
    } yield Ok(Json.toJson(response))

    result.merge
  }

  def updateRecord(eori: String, recordId: String): Action[JsValue] = authAction(eori).async(parse.json) {
    implicit request =>
      val result = for {
        _                   <- EitherT.fromEither[Future](validateAcceptHeader)
        _                   <- EitherT
                                 .fromEither[Future](validateRecordId(recordId))
                                 .leftMap(e => BadRequestErrorResponse(uuidService.uuid, Seq(e)).asPresentation)
        updateRecordRequest <-
          EitherT.fromEither[Future](validateRequestBody[UpdateRecordRequest](fieldsToErrorCode))
        response            <- putRecord(eori, recordId, updateRecordRequest)
      } yield Ok(Json.toJson(response))

      result.merge
  }

  private def patchRecord(
    eori: String,
    recordId: String,
    updateRecordRequest: PatchRecordRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, CreateOrUpdateRecordResponse] =
    EitherT(
      updateRecordService.patchRecord(eori, recordId, updateRecordRequest)
    )
      .leftMap(e => Status(e.httpStatus)(Json.toJson(e.errorResponse)))

  private def putRecord(
    eori: String,
    recordId: String,
    updateRecordRequest: UpdateRecordRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, CreateOrUpdateRecordResponse] =
    EitherT(
      updateRecordService.putRecord(eori, recordId, updateRecordRequest)
    )
      .leftMap(e => Status(e.httpStatus)(Json.toJson(e.errorResponse)))

  // TODO: After Drop 1.1 this should be removed - Ticket: TGP-1903
  private def validateClientIdIfSupported(implicit request: Request[_]) =
    if (appConfig.shouldSendClientIdHeader) validateClientId
    else Right("")
}
