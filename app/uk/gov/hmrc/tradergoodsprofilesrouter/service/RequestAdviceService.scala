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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{EisHttpErrorResponse, InternalServerErrorResponse, RequestAdviceConnector}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.RequestAdvice
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.advicerequests.{GoodsItem, TraderDetails}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GoodsItemRecords
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode

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
  )(implicit hc: HeaderCarrier): Future[Either[EisHttpErrorResponse, Int]] = {
    val correlationId = uuidService.uuid

    routerService
      .fetchRecord(eori, recordId)
      .flatMap {
        case Right(goodsItemRecord) =>
          connector
            .requestAdvice(createNewTraderDetails(eori, goodsItemRecord, request), correlationId)
            .flatMap {
              case Right(response) => Future.successful(Right(response))
              case Left(error)     => Future.successful(Left(error))
            }
        case Left(error)            => Future.successful(Left(error))
      }
      .recover { case ex: Throwable =>
        logger.error(
          s"""[RequestAdviceService] - Error when creating accreditation for
                          correlationId: $correlationId, message: ${ex.getMessage}""",
          ex
        )

        Left(
          InternalServerErrorResponse(
            ErrorResponse(correlationId, UnexpectedErrorCode, ex.getMessage)
          )
        )
      }
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
