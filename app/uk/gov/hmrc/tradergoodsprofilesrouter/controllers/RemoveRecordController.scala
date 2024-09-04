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
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.BadRequestErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.{AuthAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RemoveRecordService, UuidService}

import scala.concurrent.{ExecutionContext, Future}

class RemoveRecordController @Inject() (
  authAction: AuthAction,
  override val controllerComponents: ControllerComponents,
  service: RemoveRecordService,
  override val uuidService: UuidService,
  appConfig: AppConfig
)(implicit override val ec: ExecutionContext)
    extends BackendBaseController
    with ValidationRules
    with Logging {

  def remove(eori: String, recordId: String, actorId: String): Action[AnyContent] = authAction(eori).async {
    implicit request: Request[AnyContent] =>
      val result = for {
        _ <- validateHeadersIfSupported
        _ <- EitherT
               .fromEither[Future](validateQueryParameters(actorId, recordId))
               .leftMap(e => BadRequestErrorResponse(uuidService.uuid, e).asPresentation)
        _ <- removeRecord(eori, recordId, actorId)
      } yield NoContent

      result.merge
  }

  private def removeRecord(
    eori: String,
    recordId: String,
    actorId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, Int] =
    EitherT(
      service
        .removeRecord(eori, recordId, actorId)
    )
      .leftMap(e => Status(e.httpStatus)(Json.toJson(e.errorResponse)))

  // ToDO:  remove this validation for drop 2 - TGP-2029
  private def validateHeadersIfSupported(implicit request: Request[_]): EitherT[Future, Result, String] =
    for {
      _ <- validateClientIdIfSupported
      _ <- validateAcceptHeaderIfSupported
    } yield "Success"

  private def validateClientIdIfSupported(implicit request: Request[_]): EitherT[Future, Result, String] =
    if (appConfig.isClientIdOptional) EitherT.rightT("Success")
    else EitherT.fromEither[Future](validateClientId)

  private def validateAcceptHeaderIfSupported(implicit request: Request[_]): EitherT[Future, Result, String] =
    if (appConfig.acceptHeaderDisabled) EitherT.rightT("Success")
    else EitherT.fromEither[Future](validateAcceptHeader)

}
