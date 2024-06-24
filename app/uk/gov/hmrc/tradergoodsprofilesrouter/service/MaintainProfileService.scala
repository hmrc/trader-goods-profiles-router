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

import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{EisHttpErrorResponse, MaintainProfileConnector}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.MaintainProfileRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.MaintainProfileEisRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.MaintainProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import scala.concurrent.{ExecutionContext, Future}

class MaintainProfileService @Inject() (connector: MaintainProfileConnector, uuidService: UuidService)(implicit
  ec: ExecutionContext
) extends Logging {

  def maintainProfile(eori: String, request: MaintainProfileRequest)(implicit
    hc: HeaderCarrier
  ): Future[Either[EisHttpErrorResponse, MaintainProfileResponse]] = {
    val eisRequest    =
      MaintainProfileEisRequest(
        eori,
        request.actorId,
        request.ukimsNumber,
        request.nirmsNumber,
        padNiphlNumber(request.niphlNumber)
      )
    val correlationId = uuidService.uuid

    connector
      .maintainProfile(eisRequest, correlationId)
      .map {
        case Right(response) => Right(response)
        case Left(response)  => Left(response)
      }
      .recover { case ex: Throwable =>
        logger.error(
          s"""[MaintainProfileService] - Error when maintaining profile for ActorId: ${request.actorId},
          s"correlationId: $correlationId, message: ${ex.getMessage}""",
          ex
        )
        Left(
          EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, UnexpectedErrorCode, ex.getMessage))
        )
      }
  }

  // TODO: This will need to be removed once EIS / B&T make the same validation on their side
  private def padNiphlNumber(niphlNumber: Option[String]): Option[String] =
    niphlNumber match {
      case Some(niphl) =>
        if (niphl.length >= 8) Some(niphl)
        else Some("-" * (8 - niphl.length) + niphl)
      case None        => None
    }
}
