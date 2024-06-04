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

package uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.authorisedEnrolments
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.AuthAction.gtpEnrolmentKey
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.{InternalServerErrorCode, UnauthorizedCode}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthAction @Inject() (
  override val authConnector: AuthConnector,
  val bodyParser: BodyParsers.Default,
  uuidService: UuidService,
  cc: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions
    with Logging {

  private val fetch = authorisedEnrolments

  def apply(
    eori: String
  ): ActionBuilder[Request, AnyContent] with ActionFunction[Request, Request] =
    new ActionBuilder[Request, AnyContent] with ActionFunction[Request, Request] {

      override val parser = bodyParser

      protected def executionContext: ExecutionContext = ec

      override def invokeBlock[A](
        request: Request[A],
        block: Request[A] => Future[Result]
      ): Future[Result] = {

        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
        implicit val req: Request[A]   = request

        authorised(Enrolment(gtpEnrolmentKey))
          .retrieve(fetch) {
            case authorisedEnrolments =>
              block(request)
            case _                    =>
              //todo: not sure if this would never happens.
              throw InsufficientEnrolments()
          }
          .recover {
            case error: AuthorisationException => handleUnauthorisedError(error.reason)
            case ex: Throwable                 => handleException(request, ex)
          }
      }
    }

  private def handleUnauthorisedError[A](
    errorMessage: String
  )(implicit request: Request[A]): Result = {

    logger.error(s"[AuthAction] - Unauthorised exception for ${request.uri} with error $errorMessage")

    Unauthorized(
      Json.toJson(
        ErrorResponse(
          uuidService.uuid,
          UnauthorizedCode,
          s"Downstream error: The details signed in do not have a Trader Goods Profile"
        )
      )
    )
  }

  private def handleException[A](request: Request[A], ex: Throwable): Result = {
    logger.error(s"Internal server error for ${request.uri} with error ${ex.getMessage}", ex)

    InternalServerError(
      Json.toJson(
        ErrorResponse(
          uuidService.uuid,
          InternalServerErrorCode,
          s"Downstream error: Internal server error for ${request.uri} with error: ${ex.getMessage}"
        )
      )
    )
  }
}

object AuthAction {
  val gtpEnrolmentKey  = "HMRC-CUS-ORG"
  val gtpIdentifierKey = "EORINumber"
}
