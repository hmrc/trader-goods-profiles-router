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
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.{AuthAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.BadRequestErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{GetRecordsService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.InvalidQueryParameter

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class GetRecordsController @Inject() (
  authAction: AuthAction,
  override val controllerComponents: ControllerComponents,
  getRecordService: GetRecordsService,
  override val uuidService: UuidService
)(implicit val ec: ExecutionContext)
    extends BackendBaseController
    with ValidationRules {

  def getTGPRecord(
    eori: String,
    recordId: String
  ): Action[AnyContent] = authAction(eori).async { implicit request: Request[AnyContent] =>
    val result = for {
      _          <- EitherT.fromEither[Future](validateClientId)
      _          <- EitherT
                      .fromEither[Future](validateRecordId(recordId))
                      .leftMap(e => BadRequestErrorResponse(uuidService.uuid, Seq(e)).asPresentation)
      recordItem <- getRecordService.fetchRecord(eori, recordId)
    } yield Ok(Json.toJson(recordItem))

    result.merge
  }

  def getTGPRecords(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  ): Action[AnyContent] = authAction(eori).async { implicit request: Request[AnyContent] =>
    val result = for {
      _         <- EitherT.fromEither[Future](validateClientId)
      validDate <- validateDate(lastUpdatedDate)
      records   <- getRecordService.fetchRecords(eori, validDate, page, size)
    } yield Ok(Json.toJson(records))

    result.merge
  }

  private def validateDate(lastUpdateDate: Option[String]): EitherT[Future, Result, Option[Instant]] =
    EitherT.fromEither(
      Try(lastUpdateDate.map(Instant.parse(_))).toEither.left.map(_ =>
        BadRequest(
          Json.toJson(
            ErrorResponse(
              uuidService.uuid,
              InvalidQueryParameter,
              "Query parameter lastUpdateDate is not a date format"
            )
          )
        )
      )
    )
}
