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
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{CreateProfileConnector, EisHttpErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateProfileRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.CreateProfileEisRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.CreateProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import scala.concurrent.{ExecutionContext, Future}

class CreateProfileService @Inject() (
  connector: CreateProfileConnector,
  uuidService: UuidService
)(implicit
  ec: ExecutionContext
) extends Logging {

  def createProfile(eori: String, request: CreateProfileRequest)(implicit
    hc: HeaderCarrier
  ): Future[Either[EisHttpErrorResponse, CreateProfileResponse]] = {
    val eisRequest    =
      CreateProfileEisRequest(
        eori,
        request.actorId,
        Some(request.ukimsNumber),
        request.nirmsNumber,
        padNiphlNumber(request.niphlNumber)
      )
    val correlationId = uuidService.uuid

    connector
      .createProfile(eisRequest, correlationId)
      .map {
        case Right(_)            => Right(buildResponse(eisRequest))
        case Left(errorResponse) =>
          logger.info(
            s"""[CreateProfileService] - Eis returns ${errorResponse.httpStatus}: ${request.actorId},
            s"correlationId: $correlationId, message: ${errorResponse.errorResponse.message}"""
          )
          Left(errorResponse)
      }
      .recover { case ex: Throwable =>
        logger.error(
          s"""[CreateProfileService] - Error when creating profile for ActorId: ${request.actorId},
          s"correlationId: $correlationId, message: ${ex.getMessage}""",
          ex
        )
        Left(
          EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, UnexpectedErrorCode, ex.getMessage))
        )
      }
  }

  private def buildResponse(request: CreateProfileEisRequest) =
    CreateProfileResponse(
      request.eori,
      request.actorId,
      request.ukimsNumber,
      request.nirmsNumber,
      removeLeadingDashes(request.niphlNumber)
    )

  private def padNiphlNumber(niphlNumber: Option[String]): Option[String] =
    niphlNumber.map { niphl =>
      if (niphl.length >= 8) niphl else "-" * (8 - niphl.length) + niphl
    }

  private def removeLeadingDashes(niphlNumber: Option[String]): Option[String] =
    niphlNumber match {
      case Some(niphl) => Some(niphl.dropWhile(_ == '-'))
      case None        => None
    }
}
