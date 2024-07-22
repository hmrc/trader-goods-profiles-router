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
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{EisHttpErrorResponse, GetRecordsConnector}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.{GetRecordsResponse, GoodsItemRecords}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class GetRecordsService @Inject() (eisConnector: GetRecordsConnector, uuidService: UuidService)(implicit
  ec: ExecutionContext
) extends Logging {

  def fetchRecord(eori: String, recordId: String, url: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[EisHttpErrorResponse, GoodsItemRecords]] = {
    val correlationId: String = uuidService.uuid
    eisConnector
      .fetchRecord(eori, recordId, correlationId, url)
      .map {
        case Right(response) => Right(GoodsItemRecords(response.goodsItemRecords.head))
        case Left(error)     => Left(error)
      }
      .recover { case ex: Throwable =>
        logMessageAndReturnError(
          correlationId,
          ex,
          s"""[GetRecordsService] - Error when fetching a single record for Eori Number: $eori,
            s"recordId: $recordId, correlationId: $correlationId, message: ${ex.getMessage}"""
        )
      }

  }

  def fetchRecords(
    eori: String,
    lastUpdatedDate: Option[Instant] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit
    hc: HeaderCarrier
  ): Future[Either[EisHttpErrorResponse, GetRecordsResponse]] = {
    val correlationId: String = uuidService.uuid
    eisConnector
      .fetchRecords(eori, correlationId, lastUpdatedDate, page, size)
      .map {
        case Right(response) => Right(GetRecordsResponse(response))
        case Left(response)  => Left(response)
      }
      .recover { case ex: Throwable =>
        logMessageAndReturnError(
          correlationId,
          ex,
          s"""[GetRecordsService] - Error when fetching records for Eori Number: $eori,
            s"correlationId: $correlationId, message: ${ex.getMessage}"""
        )
      }
  }

  private def logMessageAndReturnError(correlationId: String, ex: Throwable, logMsg: String) = {
    logger.error(logMsg, ex)
    Left(
      EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, UnexpectedErrorCode, ex.getMessage))
    )
  }
}
