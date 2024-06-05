package uk.gov.hmrc.tradergoodsprofilesrouter.service

import cats.data.EitherT
import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.MaintainProfileRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.MaintainProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import scala.concurrent.{ExecutionContext, Future}

class MaintainProfileService @Inject() (eisConnector: EISConnector, uuidService: UuidService)(implicit
  ec: ExecutionContext
) extends Logging {

  def maintainProfile(request: MaintainProfileRequest)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, MaintainProfileResponse] = {
    val correlationId = uuidService.uuid
    EitherT(
      eisConnector
        .maintainProfile(request, correlationId)
        .map {
          case response @ Right(_) => response
          case error @ Left(_)     => error
        }
        .recover { case ex: Throwable =>
          logger.error(
            s"""[MaintainProfileService] - Error when maintaining profile for ActorId: ${request.actorId},
          s"correlationId: $correlationId, message: ${ex.getMessage}""",
            ex
          )
          Left(
            InternalServerError(
              toJson(ErrorResponse(correlationId, UnexpectedErrorCode, ex.getMessage))
            )
          )
        }
    )
  }
}
