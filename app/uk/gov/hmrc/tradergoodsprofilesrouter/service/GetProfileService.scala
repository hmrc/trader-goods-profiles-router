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
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{EisHttpErrorResponse, GetProfileConnector}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.ProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import scala.concurrent.{ExecutionContext, Future}

class GetProfileService @Inject() (
  connector: GetProfileConnector,
  uuidService: UuidService
)(implicit ec: ExecutionContext)
    extends Logging {
  def getProfile(eori: String)(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, ProfileResponse]] = {
    val correlationId = uuidService.uuid

    connector
      .get(eori, correlationId)
      .collect {
        case successResponse @ Right(_)  => successResponse
        case errorResponse @ Left(error) =>
          logger.warn(s"""[GetProfileService] - Error when retrieving profile for eori: $eori,
             correlationId: $correlationId, status: ${error.httpStatus},
             body: ${Json.toJson(error.errorResponse.toString)}""".stripMargin)
          errorResponse
      }
      .recover { case ex: Throwable =>
        logger.warn(s"""[GetProfileService] - Exception when retrieving profile for eori: $eori,
               correlationId: $correlationId, message: ${ex.getMessage}""".stripMargin)

        Left(
          EisHttpErrorResponse(
            INTERNAL_SERVER_ERROR,
            ErrorResponse(correlationId, UnexpectedErrorCode, ex.getMessage)
          )
        )
      }
  }

}
