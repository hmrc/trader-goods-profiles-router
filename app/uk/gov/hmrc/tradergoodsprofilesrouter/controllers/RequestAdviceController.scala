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
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.{AuthAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.{BadRequestErrorResponse, fieldsToErrorCode}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.RequestAdvice
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{RequestAdviceService, UuidService}

import scala.concurrent.{ExecutionContext, Future}

class RequestAdviceController @Inject() (
  authAction: AuthAction,
  override val controllerComponents: ControllerComponents,
  service: RequestAdviceService,
  override val uuidService: UuidService
)(implicit
  val ec: ExecutionContext
) extends BackendBaseController
    with ValidationRules
    with Logging {

  def requestAdvice(eori: String, recordId: String): Action[JsValue] = authAction(eori).async(parse.json) {
    implicit request: Request[JsValue] =>
      val result = for {
        _                    <- EitherT.fromEither[Future](validateClientId)
        _                    <- EitherT
                                  .fromEither[Future](validateRecordId(recordId))
                                  .leftMap(e => BadRequestErrorResponse(uuidService.uuid, Seq(e)).asPresentation)
        requestAdviceRequest <- EitherT.fromEither[Future](validateRequestBody[RequestAdvice](fieldsToErrorCode))
        _                    <- requestAdvice(eori, recordId, requestAdviceRequest)
      } yield Created

      result.merge
  }

  private def requestAdvice(
    eori: String,
    recordId: String,
    request: RequestAdvice
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, Int] =
    service.requestAdvice(eori, recordId, request).leftMap(e => Status(e.httpStatus)(Json.toJson(e.errorResponse)))

}
