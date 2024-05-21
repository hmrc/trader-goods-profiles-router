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
import com.google.inject.{ImplementedBy, Inject}
import play.api.Logging
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.CreateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.CreateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{GetEisRecordsResponse, GoodsItemRecords}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[RouterServiceImpl])
trait RouterService {
  def fetchRecord(
    eori: String,
    recordId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, GoodsItemRecords]

  def fetchRecords(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, GetEisRecordsResponse]

  def createRecord(
    request: CreateRecordRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, CreateRecordResponse]

  def removeRecord(
    eori: String,
    recordId: String,
    actorId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): EitherT[Future, Result, Int]

}

class RouterServiceImpl @Inject() (eisConnector: EISConnector, uuidService: UuidService)(implicit ec: ExecutionContext)
    extends RouterService
    with Logging {

  override def fetchRecord(eori: String, recordId: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, GoodsItemRecords] = {
    val correlationId: String = uuidService.uuid
    EitherT(
      eisConnector
        .fetchRecord(eori, recordId, correlationId)
        .map {
          case Right(response) => Right(response.goodsItemRecords.head)
          case Left(error)     => Left(error)
        }
        .recover { case ex: Throwable =>
          logger.error(
            s"""[RouterService] - Error when fetching a single record for Eori Number: $eori,
            s"recordId: $recordId, correlationId: $correlationId, message: ${ex.getMessage}""",
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

  override def fetchRecords(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, GetEisRecordsResponse] = {
    val correlationId: String = uuidService.uuid
    EitherT(
      eisConnector
        .fetchRecords(eori, correlationId, lastUpdatedDate, page, size)
        .map {
          case Right(response) => Right(response)
          case Left(error)     => Left(error)
        }
        .recover { case ex: Throwable =>
          logger.error(
            s"""[RouterService] - Error when fetching records for Eori Number: $eori,
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

  override def createRecord(
    request: CreateRecordRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, CreateRecordResponse] = {
    val correlationId = uuidService.uuid
    EitherT(
      eisConnector
        .createRecord(request, correlationId)
        .map {
          case response @ Right(_) => response
          case error @ Left(_)     => error
        }
        .recover { case ex: Throwable =>
          logger.error(
            s"""[RouterService] - Error when creating records for Eori Number: ${request.eori},
            correlationId: $correlationId, message: ${ex.getMessage}""",
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

  override def removeRecord(eori: String, recordId: String, actorId: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, Result, Int] = {
    val correlationId = uuidService.uuid
    EitherT(
      eisConnector
        .removeRecord(eori, recordId, actorId, correlationId)
        .map {
          case Right(_)    => Right(OK)
          case Left(error) => Left(error)
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
