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

import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

trait EISConnector {
  def fetchRecords(
      eori: String,
      recordId: Option[String],
      lastUpdatedDate: Option[String],
      page: Option[Int],
      size: Option[Int]
  )(implicit ec: ExecutionContext): Future[JsValue]
}

class EISConnectorImpl extends EISConnector {

  override def fetchRecords(
      eori: String,
      recordId: Option[String],
      lastUpdatedDate: Option[String],
      page: Option[Int],
      size: Option[Int]
  )(implicit ec: ExecutionContext): Future[JsValue] = {
    recordId match {
      case Some(id) =>
        Future.successful(
          Json.obj(
            "status" -> "success",
            "message" -> "EIS record retrieved successfully",
            "eori" -> eori,
            "recordId" -> recordId
          )
        )
      case None =>
        Future.successful(
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
  }

}
