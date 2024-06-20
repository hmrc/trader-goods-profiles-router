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
import play.api.mvc.Results.{Conflict, InternalServerError}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.RequestAdviceConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.RequestAdvice
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.advicerequests.{GoodsItem, TraderDetails}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GoodsItemRecords
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.AdviceStatuses.AllowedAdviceStatuses
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.{AdviceRequestRejectionMessage, BadRequestCode, BadRequestMessage, InvalidRequestAdviceNumberCode, InvalidRequestParameters, UnexpectedErrorCode}

import scala.concurrent.{ExecutionContext, Future}

class RequestAdviceService @Inject() (
  connector: RequestAdviceConnector,
  routerService: GetRecordsService,
  uuidService: UuidService
)(implicit ec: ExecutionContext)
    extends Logging {

  def requestAdvice(
    eori: String,
    recordId: String,
    request: RequestAdvice
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, Int] =
    for {
      goodsItemRecord <- routerService.fetchRecord(eori, recordId)
      _               <- EitherT
                           .fromEither[Future](isValidAdviceStatus(goodsItemRecord.adviceStatus))
                           .leftMap(e => e)
      _               <- connectorRequest(createNewTraderDetails(eori, goodsItemRecord, request))
    } yield CREATED

  private def connectorRequest(
    request: TraderDetails
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, Int] = {
    val correlationId = uuidService.uuid
    EitherT(
      connector
        .requestAdvice(request, correlationId)
        .map {
          case Right(_)        => Right(CREATED)
          case error @ Left(_) => error
        }
        .recover { case ex: Throwable =>
          logger.error(
            s"""[RequestAdviceService] - Error when creating accreditation for
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

  private def isValidAdviceStatus(adviceStatus: String): Either[Result, Unit] =
    if (AllowedAdviceStatuses.contains(adviceStatus.toLowerCase)) {
      Right(())
    } else {
      Left(
        Conflict(
          Json.toJson(
            ErrorResponse(
              uuidService.uuid,
              BadRequestCode,
              BadRequestMessage,
              Some(
                Seq(
                  Error(InvalidRequestParameters, AdviceRequestRejectionMessage, InvalidRequestAdviceNumberCode.toInt)
                )
              )
            )
          )
        )
      )
    }

  private def createNewTraderDetails(
    eori: String,
    goodsItemRecords: GoodsItemRecords,
    request: RequestAdvice
  ): TraderDetails = {

    val goodsItem     = GoodsItem(
      goodsItemRecords.recordId,
      goodsItemRecords.traderRef,
      goodsItemRecords.goodsDescription,
      Some(goodsItemRecords.countryOfOrigin),
      goodsItemRecords.supplementaryUnit,
      Some(goodsItemRecords.category),
      goodsItemRecords.measurementUnit,
      goodsItemRecords.comcode
    )
    val traderDetails = TraderDetails(
      eori,
      request.requestorName,
      Some(goodsItemRecords.actorId),
      request.requestorEmail,
      goodsItemRecords.ukimsNumber,
      Seq(goodsItem)
    )
    traderDetails
  }
}
