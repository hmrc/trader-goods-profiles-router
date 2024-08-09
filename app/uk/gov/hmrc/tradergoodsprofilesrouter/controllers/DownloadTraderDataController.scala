package uk.gov.hmrc.tradergoodsprofilesrouter.controllers

import cats.data.EitherT
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.{AuthAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{DownloadTraderDataService, UuidService}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DownloadTraderDataController @Inject() (
  authAction: AuthAction,
  service: DownloadTraderDataService,
  override val controllerComponents: ControllerComponents,
  override val uuidService: UuidService
)(implicit val ec: ExecutionContext)
    extends BackendBaseController
    with ValidationRules {

  def requestDataDownload(eori: String): Action[AnyContent] = authAction(eori).async {
    implicit request: Request[AnyContent] =>
      val result = for {
        _ <- EitherT(
               service.requestDownload(eori)
             )
               .leftMap(e => Status(e.httpStatus)(Json.toJson(e.errorResponse)))
      } yield Accepted

      result.merge
  }

}
