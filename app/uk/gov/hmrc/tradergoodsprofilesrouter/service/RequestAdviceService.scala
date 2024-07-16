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
import play.api.http.Status.{CONFLICT, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{EisHttpErrorResponse, RequestAdviceConnector}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.RequestAdvice
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.advicerequests.{GoodsItem, TraderDetails}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.{AdviceStatus, GoodsItemRecords}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants._

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
  )(implicit hc: HeaderCarrier): EitherT[Future, EisHttpErrorResponse, Int] = {
    val correlationId = uuidService.uuid

    for {
      goodsItemRecords <- EitherT(routerService.fetchRecord(eori, recordId, false)).leftMap(o => o)
      response         <- EitherT(validateAndRequestAdvice(eori, goodsItemRecords, request, correlationId)).leftMap(o => o)
    } yield response

  }

  private def validateAndRequestAdvice(
    eori: String,
    goodsItemRecord: GoodsItemRecords,
    request: RequestAdvice,
    correlationId: String
  )(implicit
    hc: HeaderCarrier
  ): Future[Either[EisHttpErrorResponse, Int]] =
    if (AdviceStatus.AllowedAdviceStatuses.contains(goodsItemRecord.adviceStatus)) {
      val traderDetails = createNewTraderDetails(eori, goodsItemRecord, request)
      connector
        .requestAdvice(traderDetails, correlationId)
        .map(r => r)
        .recover { case ex: Throwable =>
          logger.error(
            s"""[RequestAdviceService] - Error when creating accreditation for
                          correlationId: $correlationId, message: ${ex.getMessage}""",
            ex
          )

          Left(
            EisHttpErrorResponse(
              INTERNAL_SERVER_ERROR,
              ErrorResponse(correlationId, UnexpectedErrorCode, ex.getMessage)
            )
          )
        }
    } else
      Future.successful(
        Left(
          EisHttpErrorResponse(
            CONFLICT,
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

  private def createNewTraderDetails(
    eori: String,
    goodsItemRecords: GoodsItemRecords,
    request: RequestAdvice
  ): TraderDetails = {

    val goodsItem = GoodsItem(
      goodsItemRecords.recordId,
      goodsItemRecords.traderRef,
      goodsItemRecords.goodsDescription,
      Some(goodsItemRecords.countryOfOrigin),
      goodsItemRecords.supplementaryUnit,
      Some(goodsItemRecords.category),
      goodsItemRecords.measurementUnit,
      goodsItemRecords.comcode
    )
    TraderDetails(
      eori,
      request.requestorName,
      Some(request.actorId),
      request.requestorEmail,
      goodsItemRecords.ukimsNumber,
      Seq(goodsItem)
    )
  }
}
