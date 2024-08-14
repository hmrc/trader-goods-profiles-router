package uk.gov.hmrc.tradergoodsprofilesrouter.controllers

import cats.data.EitherT
import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.{AuthAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.fieldsToErrorCode
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateProfileRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.CreateProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{CreateProfileService, UuidService}

import scala.concurrent.{ExecutionContext, Future}

class CreateProfileController @Inject() (
                                          authAction: AuthAction,
                                          override val controllerComponents: ControllerComponents,
                                          createProfileService: CreateProfileService,
                                          appConfig: AppConfig,
                                          override val uuidService: UuidService
                                        )(implicit val ec: ExecutionContext)
  extends BackendBaseController
    with ValidationRules
    with Logging {

  def create(eori: String): Action[JsValue] = authAction(eori).async(parse.json) { implicit request =>
    println(request)
    println(eori)
    val result = for {
      _                      <- EitherT.fromEither[Future](validateClientIdIfSupported)
      _                      <- EitherT.fromEither[Future](validateAcceptHeader)
      createProfileRequest <-
        EitherT.fromEither[Future](validateRequestBody[CreateProfileRequest](fieldsToErrorCode))
      response               <- createProfile(eori, createProfileRequest)
    } yield Ok(Json.toJson(response))

    result.merge
  }

  private def createProfile(
                               eori: String,
                               createProfileRequest: CreateProfileRequest
                             )(implicit hc: HeaderCarrier): EitherT[Future, Result, CreateProfileResponse] =
    EitherT(
      createProfileService.createProfile(eori, createProfileRequest)
    )
      .leftMap(e => Status(e.httpStatus)(Json.toJson(e.errorResponse)))

  // TODO: After Drop 1.1 this should be removed - Ticket: TGP-2014
  private def validateClientIdIfSupported(implicit request: Request[_]) =
    if (!appConfig.isDrop1_1_enabled) validateClientId
    else Right("")
}
