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
import com.google.inject.Inject
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.WithdrawAdviceConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.BadRequestErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.{AuthAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WithdrawAdviceController @Inject() (
  authAction: AuthAction,
  withdrawAdviceConnector: WithdrawAdviceConnector,
  override val uuidService: UuidService,
  override val controllerComponents: ControllerComponents
)(implicit override val ec: ExecutionContext)
    extends BackendBaseController
    with ValidationRules {

  def withdrawAdvice(eori: String, recordId: String): Action[JsValue] = authAction(eori).async(parse.json) {
    implicit request =>
      val result = for {
        _               <- EitherT.fromEither[Future](validateClientId)
        _               <- EitherT.fromEither[Future](validateAcceptHeader)
        validatedParams <- EitherT
                             .fromEither[Future](validateWithdrawAdviceQueryParam(recordId))
                             .leftMap(e => BadRequestErrorResponse(uuidService.uuid, e).asPresentation)
        _               <- EitherT(withdrawAdviceConnector.put(recordId, validatedParams.withdrawReason))
                             .leftMap(e => Status(e.httpStatus)(Json.toJson(e.errorResponse)))
      } yield NoContent

      result.merge
  }

}
