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

package uk.gov.hmrc.tradergoodsprofilesrouter.controllers

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.fieldsToErrorCode
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.RequestAccreditation
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.accreditationrequests.{GoodsItem, TraderDetails}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GoodsItemRecords
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{AccreditationService, GetRecordsService, UuidService}

import scala.concurrent.{ExecutionContext, Future}

class RequestAccreditationController @Inject() (
  override val controllerComponents: ControllerComponents,
  getRecordService: GetRecordsService,
  accreditationService: AccreditationService,
  override val uuidService: UuidService
)(implicit
  val ec: ExecutionContext
) extends BackendBaseController
    with ValidationRules
    with Logging {

  def requestAccreditation: Action[JsValue] = Action.async(parse.json) { implicit request =>
    val result = for {

      requestAccreditationRequest <-
        EitherT.fromEither[Future](validateRequestBody[RequestAccreditation](fieldsToErrorCode))
      recordItem                  <- getRecordService.fetchRecord(requestAccreditationRequest.eori, requestAccreditationRequest.recordId)
      newAccreditationRequest      = createNewTraderDetails(recordItem, requestAccreditationRequest)
      _                           <- accreditationService.requestAccreditation(newAccreditationRequest)
    } yield Created

    result.merge
  }

  private def createNewTraderDetails(
    goodsItemRecords: GoodsItemRecords,
    request: RequestAccreditation
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
      request.eori,
      request.requestorName,
      Some(goodsItemRecords.actorId),
      request.requestorEmail,
      goodsItemRecords.ukimsNumber,
      Seq(goodsItem)
    )
    traderDetails
  }

}
