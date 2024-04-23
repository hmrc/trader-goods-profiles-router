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

case class GetRecordsController(cc: ControllerComponents)
    extends AbstractController(cc) {

  def getTGPRecords(
      eori: String,
      lastUpdatedDate: Option[String] = None,
      page: Option[Int] = None,
      size: Option[Int] = None
  ): Action[AnyContent] = Action { implicit request =>
    Ok(
      Json.obj(
        "status" -> "success",
        "message" -> "EIS list of records retrieved successfully",
        "eori" -> eori,
        "lastUpdatedDate" -> lastUpdatedDate,
        "page" -> page,
        "size" -> size
      )
    )
  }

  def getSingleTGPRecord(
      eori: String,
      recordId: String,
      lastUpdatedDate: Option[String] = None,
      page: Option[Int] = None,
      size: Option[Int] = None
  ): Action[AnyContent] = Action { implicit request =>
    Ok(
      Json.obj(
        "status" -> "success",
        "message" -> "EIS record retrieved successfully",
        "eori" -> eori,
        "recordId" -> recordId,
        "lastUpdatedDate" -> lastUpdatedDate,
        "page" -> page,
        "size" -> size
      )
    )
  }
}
