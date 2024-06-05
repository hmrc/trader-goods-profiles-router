package uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action

import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, BodyParser, ControllerComponents, Request, Result}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions, Enrolment, Enrolments}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.AuthAction.{EnrolmentKey, IdentifierKey}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.{ForbiddenCode, InternalServerErrorCode, UnauthorizedCode}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthActionX @Inject() (
  override val authConnector: AuthConnector,
  uuidService: UuidService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions
    with Logging {

  def authorisedActionWithEori[A](bodyParser: BodyParser[A], eori: String)(block: Request[A] => Future[Result]): Action[A] =
    Action.async(bodyParser) { implicit request =>
      authorised(Enrolment(EnrolmentKey))
        .retrieve(Retrievals.authorisedEnrolments) { enrolments: Enrolments =>
          getEoriNumber(enrolments) match {
            case Some(identifier) if identifier.equals(eori) => block(request)
            case _                                           => createForbiddenError
          }
        }
        .recover {
          case error: AuthorisationException => handleUnauthorisedError(error.reason)
          case ex: Throwable                 => handleException(request, ex)
        }
    }

  def authorisedAction[A](bodyParser: BodyParser[A])(block: Request[A] => Future[Result]): Action[A] =
    Action.async(bodyParser) { implicit request =>

      request.body match {
      }
    }
      authorised(Enrolment(EnrolmentKey))
        .retrieve(Retrievals.authorisedEnrolments) { enrolments: Enrolments =>
          getEoriNumber(enrolments) match {
            case Some(identifier) if identifier.equals(eori) => block(request)
            case _                                           => createForbiddenError
          }
        }
        .recover {
          case error: AuthorisationException => handleUnauthorisedError(error.reason)
          case ex: Throwable                 => handleException(request, ex)
        }

  def getEoriNumber(enrolments: Enrolments): Option[String] =
    for {
      enrolment  <- enrolments.getEnrolment(EnrolmentKey)
      identifier <- enrolment.getIdentifier(IdentifierKey)
    } yield identifier.value

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

  private def createForbiddenError: Future[Result] =
    Future.successful(
      Forbidden(
        Json.toJson(
          ErrorResponse(
            uuidService.uuid,
            ForbiddenCode,
            s"Downstream error: EORI number is incorrect"
          )
        )
      )
    )
}
