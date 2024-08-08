package uk.gov.hmrc.tradergoodsprofilesrouter.controllers

import cats.data.EitherT
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.BadRequestErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.{AuthAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DownloadTraderDataController @Inject() (
  authAction: AuthAction,
  override val controllerComponents: ControllerComponents,
  appConfig: AppConfig,
  override val uuidService: UuidService
)(implicit val ec: ExecutionContext)
    extends BackendBaseController
    with ValidationRules {

  def requestDataDownload(eori: String): Action[AnyContent] = authAction(eori).async {
    implicit request: Request[AnyContent] =>
      val result = for {
        _ <- EitherT.fromEither[Future](validateClientId)
        _ <- EitherT.fromEither[Future](validateAcceptHeader)
      } yield Accepted

      result.merge
  }
}
