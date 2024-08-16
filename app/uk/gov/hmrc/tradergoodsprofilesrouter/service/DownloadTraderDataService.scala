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
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{DownloadTraderDataConnector, EisHttpErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import scala.concurrent.{ExecutionContext, Future}

class DownloadTraderDataService @Inject() (
  connector: DownloadTraderDataConnector,
  uuidService: UuidService
)(implicit ec: ExecutionContext)
    extends Logging {

  def requestDownload(eori: String)(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, Int]] = {
    val correlationId = uuidService.uuid
    connector
      .requestDownload(eori, correlationId)
      .map {
        case Right(_)       => Right(ACCEPTED)
        case Left(response) =>
          logger.warn(
            s"""[DownloadTraderDataService] - unexpected response from EIS: $eori correlationId: $correlationId, response: $response"""
          )
          Left(response)
      } recover { case e: Throwable =>
      logger.error(
        s"""[DownloadTraderDataService] - Error occurred while requesting download of trader data for Eori: $eori correlationId: $correlationId, message: ${e.getMessage}""",
        e
      )
      Left(EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, UnexpectedErrorCode, e.getMessage)))
    }
  }
}
