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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofilesrouter.config.AppConfig
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.ValidationRules.BadRequestErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action.{AuthAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.{GetRecordsResponse, GoodsItemRecords}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{GetRecordsService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ApplicationConstants.{InvalidQueryParameter, InvalidSizeCode}

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class GetRecordsController @Inject() (
  authAction: AuthAction,
  override val controllerComponents: ControllerComponents,
  getRecordService: GetRecordsService,
  appConfig: AppConfig,
  override val uuidService: UuidService
)(implicit val ec: ExecutionContext)
    extends BackendBaseController
    with ValidationRules {

  def getTGPRecord(
    eori: String,
    recordId: String
  ): Action[AnyContent] = authAction(eori).async { implicit request: Request[AnyContent] =>
    val result = for {
      _          <- EitherT.fromEither[Future](validateClientIdIfSupported)
      _          <- EitherT.fromEither[Future](validateAcceptHeader)
      _          <- EitherT
                      .fromEither[Future](validateRecordId(recordId))
                      .leftMap(e => BadRequestErrorResponse(uuidService.uuid, Seq(e)).asPresentation)
      recordItem <- getSingleRecord(eori, recordId)
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
      _         <- EitherT.fromEither[Future](validateClientIdIfSupported)
      _         <- EitherT.fromEither[Future](validateAcceptHeader)
      validDate <- validateDate(lastUpdatedDate)
      validSize <- validateMaxSize(size)
      records   <- getRecords(eori, validSize, page, validDate)
    } yield Ok(Json.toJson(records))

    result.merge
  }

  private def validateMaxSize(size: Option[Int]): EitherT[Future, Result, Int] =
    size match {
      case Some(size) if size > appConfig.hawkConfig.getRecordsMaxSize =>
        EitherT.leftT(
          BadRequest(
            Json.toJson(
              ErrorResponse(
                uuidService.uuid,
                InvalidSizeCode,
                s"Invalid query parameter size, max allowed size is : ${appConfig.hawkConfig.getRecordsMaxSize}"
              )
            )
          )
        )
      case _                                                           => EitherT.right(Future.successful(size.getOrElse(appConfig.hawkConfig.getRecordsDefaultSize)))
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

  private def getRecords(
    eori: String,
    size: Int,
    page: Option[Int],
    validDate: Option[Instant]
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, GetRecordsResponse] =
    EitherT(
      getRecordService.fetchRecords(eori, size, page, validDate)
    )
      .leftMap(e => Status(e.httpStatus)(Json.toJson(e.errorResponse)))

  private def getSingleRecord(
    eori: String,
    recordId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, GoodsItemRecords] =
    EitherT(
      getRecordService.fetchRecord(eori, recordId, appConfig.hawkConfig.getRecordsUrl)
    )
      .leftMap(e => Status(e.httpStatus)(Json.toJson(e.errorResponse)))

  // TODO: After Drop 1.1 this should be removed - Ticket: TGP-2014
  private def validateClientIdIfSupported(implicit request: Request[_]) =
    if (!appConfig.isClientIdOptional) validateClientId
    else Right("")
}
