package uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action

import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, Reads}
import play.api.mvc.{Action, BodyParser, ControllerComponents, Request, Result}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions, Enrolment, Enrolments}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.AuthAction.{EnrolmentKey, IdentifierKey}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.{ForbiddenCode, InternalServerErrorCode, UnauthorizedCode}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

class ValidateRequestBodyAction

@Singleton
class ValidateRequestBodyAction @Inject() (
                              override val authConnector: AuthConnector,
                              uuidService: UuidService,
                              cc: ControllerComponents
                            )(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with AuthorisedFunctions
    with Logging {

  def authorisedActionWithEori[A](bodyParser: BodyParser[A], eori: String)(block: Request[A] => Future[Result]): Action[A] =
    Action.async(bodyParser) { implicit request =>
      request.body
    }

  def authorisedAction[A](bodyParser: BodyParser[A])(block: Request[A] => Future[Result]): Action[A] =
    Action.async(bodyParser) { implicit request =>

      request.body match {
      }
    }

  def parsingJson[T](implicit rds: Reads[T]): BodyParser[T] =
    parse.json.validate { json =>
      json.validate[T] match {
        case JsSuccess(value, _) => Right(value)
        case JsError(error) =>
          val errorResponse = Json.toJson(ErrorResponse(BAD_REQUEST, "Bad Request"))
          logger.warn(s"Bad Request [$errorResponse]")
          logger.warn(s"Errors: [$error]")
          Left(BadRequest(errorResponse))
      }
    }

}
