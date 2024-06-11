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
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.RemoveRecordConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import scala.concurrent.{ExecutionContext, Future}

class RemoveRecordService @Inject() (
  connector: RemoveRecordConnector,
  uuidService: UuidService
)(implicit ec: ExecutionContext)
    extends Logging {
  def removeRecord(eori: String, recordId: String, actorId: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, Int] = {
    val correlationId = uuidService.uuid
    EitherT(
      connector
        .removeRecord(eori, recordId, actorId, correlationId)
        .map {
          case Right(_)        => Right(NO_CONTENT)
          case error @ Left(_) => error
        }
        .recover { case ex: Throwable =>
          logger.error(
            s"""[RouterService] - Error occurred while removing record for Eori Number: $eori, recordId: $recordId,
            actorId: $actorId, correlationId: $correlationId, message: ${ex.getMessage}""",
            ex
          )

          Left(
            InternalServerError(
              Json.toJson(ErrorResponse(correlationId, UnexpectedErrorCode, ex.getMessage))
            )
          )
        }
    )
  }
}
