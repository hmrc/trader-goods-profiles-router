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

import play.api.libs.json.Json
import play.api.mvc.{
  AbstractController,
  Action,
  AnyContent,
  ControllerComponents
}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector

import scala.concurrent.ExecutionContext

case class GetRecordsController(
    cc: ControllerComponents,
    eisConnector: EISConnector
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def getTGPRecords(
      eori: String,
      lastUpdatedDate: Option[String] = None,
      page: Option[Int] = None,
      size: Option[Int] = None
  ): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    eisConnector.fetchRecords(eori, lastUpdatedDate, page, size, hc).map {
      jsonResponse =>
        Ok(jsonResponse)
    }
  }

//  def getSingleTGPRecord(
//      eori: String,
//      recordId: String
//  ): Action[AnyContent] = Action.async { implicit request =>
//    eisConnector.fetchRecord(eori, Some(recordId), None, None, None).map {
//      jsonResponse =>
//        Ok(jsonResponse)
//    }
//  }
}
