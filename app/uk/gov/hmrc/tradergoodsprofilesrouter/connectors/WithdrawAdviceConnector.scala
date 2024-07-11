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

package uk.gov.hmrc.tradergoodsprofilesrouter.connectors

import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.StatusHttpReader
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.Error._
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{DateTimeService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.UnexpectedErrorCode
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.WithdrawAdviceConstant._

import scala.concurrent.{ExecutionContext, Future}

class WithdrawAdviceConnector @Inject() (
  override val appConfig: AppConfig,
  httpClientV2: HttpClientV2,
  uuidService: UuidService,
  override val dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext)
    extends BaseConnector
    with EisHttpErrorHandler
    with Logging {

  def delete(recordId: String, withdrawReason: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[EisHttpErrorResponse, Int]] = {
    val url = appConfig.pegaConfig.getWithdrawAdviseUrl

    val correlationId = uuidService.uuid

    httpClientV2
      .put(url"$url")
      .setHeader(
        buildHeaders(
          correlationId,
          appConfig.pegaConfig.getWithdrawAdviceBearerToken,
          appConfig.pegaConfig.forwardedHost
        ): _*
      )
      .withBody(createPayload(recordId, withdrawReason))
      .execute(StatusHttpReader(correlationId, handleErrorResponse), ec)
      .recover { case ex: Throwable =>
        logger.error(
          s"[WithdrawAdviceConnector] - Error withdrawing Advice, recordId $recordId, message ${ex.getMessage}",
          ex
        )

        Left(
          EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, UnexpectedErrorCode, ex.getMessage))
        )
      }
  }

  private def createPayload(publicRecordID: String, withdrawReason: String): JsObject =
    Json.obj(
      "withdrawRequest" -> Json.obj(
        "requestDetail" -> Json.obj(
          "withdrawDetail" -> Json.obj(
            "withdrawDate"   -> dateTimeService.timestamp.asStringSeconds,
            "withdrawReason" -> withdrawReason
          ),
          "goodsItems"     -> Json.arr(Json.obj("publicRecordID" -> publicRecordID))
        )
      )
    )

  override def parseFaultDetail(rawDetail: String, correlationId: String): Option[errors.Error] = {
    val regex = """error:\s*(\w+),\s*message:\s*(.*)""".r
    regex
      .findFirstMatchIn(rawDetail)
      .map(_ group 1)
      .collect {
        case InvalidOrMissingCorrelationIdCode =>
          invalidRequestParameterError(InvalidOrMissingCorrelationIdMsg, 1)
        case InvalidOrMissingForwardedHostCode =>
          invalidRequestParameterError(InvalidOrMissingForwardedHostMsg, 5)
        case InvalidOrMissingContentTypeCode   =>
          invalidRequestParameterError(InvalidOrMissingContentTypeMsg, 3)
        case InvalidOrMissingAcceptCode        =>
          invalidRequestParameterError(InvalidOrMissingAcceptMsg, 4)
        case InvalidWithdrawDateCode           =>
          invalidRequestParameterError(InvalidWithdrawDateMsg, 1013)
        case InvalidGoodsItemsCode             =>
          invalidRequestParameterError(InvalidGoodsItemsMsg, 1014)
        case MissingRecordIdCode               => invalidRequestParameterError(InvalidRecordIdMsg, 25)
        case DecisionAlreadyMadeCode           => invalidRequestParameterError(InvalidRecordIdMsg, 1017)
        //todo: add "case _"
      }
  }

}
