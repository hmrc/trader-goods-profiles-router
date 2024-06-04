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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.request.eis.accreditationrequests

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, OWrites, Reads}
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ValidationSupport.Reads.lengthBetween
import scala.Function.unlift

case class RequestAccreditation(
  eori: String,
  requestorName: String,
  recordId: String,
  requestorEmail: String
)

object RequestAccreditation {

  implicit val reads: Reads[RequestAccreditation] =
    ((JsPath \ "eori").read(lengthBetween(14, 17)) and
      (JsPath \ "requestorName").read(lengthBetween(1, 70)) and
      (JsPath \ "recordId").read(lengthBetween(36, 36)) and
      (JsPath \ "requestorEmail")
        .read(lengthBetween(3, 254)))(RequestAccreditation.apply _)

  implicit lazy val writes: OWrites[RequestAccreditation] =
    ((JsPath \ "eori").write[String] and
      (JsPath \ "requestorName").write[String] and
      (JsPath \ "recordId").write[String] and
      (JsPath \ "requestorEmail").write[String])(unlift(RequestAccreditation.unapply))
}
