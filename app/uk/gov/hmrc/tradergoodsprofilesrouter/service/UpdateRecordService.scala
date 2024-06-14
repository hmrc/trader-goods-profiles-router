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
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.UpdateRecordConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.UpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.payloads.UpdateRecordPayload
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateRecordService @Inject() (
  connector: UpdateRecordConnector,
  uuidService: UuidService
)(implicit ec: ExecutionContext)
    extends Logging {

  def updateRecord(
    eori: String,
    recordId: String,
    request: UpdateRecordRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, CreateOrUpdateRecordResponse] = {
    val correlationId = uuidService.uuid
    val payload       = UpdateRecordPayload(eori, recordId, request)
    EitherT(
      connector
        .updateRecord(payload, correlationId)
        .map {
          case response @ Right(_) => response
          case error @ Left(_)     => error
        }
        .recover { case ex: Throwable =>
          logger.error(
            s"""[UpdateRecordService] - Error when updating records for Eori Number: $eori,
            s"correlationId: $correlationId, message: ${ex.getMessage}""",
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
