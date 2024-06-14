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

package uk.gov.hmrc.tradergoodsprofilesrouter.service

import cats.data.EitherT
import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status.CREATED
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.AccreditationConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.accreditationrequests.TraderDetails
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import scala.concurrent.{ExecutionContext, Future}

class AccreditationService @Inject() (connector: AccreditationConnector, uuidService: UuidService)(implicit
  ec: ExecutionContext
) extends Logging {

  def requestAccreditation(
    request: TraderDetails
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, Int] = {
    val correlationId = uuidService.uuid
    EitherT(
      connector
        .requestAccreditation(request, correlationId)
        .map {
          case Right(_)        => Right(CREATED)
          case error @ Left(_) => error
        }
        .recover { case ex: Throwable =>
          logMessageAndReturnError(
            correlationId,
            ex,
            s"""[AccreditationService] - Error when creating accreditation for
            correlationId: $correlationId, message: ${ex.getMessage}"""
          )
        }
    )
  }

  private def logMessageAndReturnError(correlationId: String, ex: Throwable, logMsg: String) = {
    logger.error(logMsg, ex)
    Left(
      InternalServerError(
        Json.toJson(ErrorResponse(correlationId, UnexpectedErrorCode, ex.getMessage))
      )
    )
  }

}
