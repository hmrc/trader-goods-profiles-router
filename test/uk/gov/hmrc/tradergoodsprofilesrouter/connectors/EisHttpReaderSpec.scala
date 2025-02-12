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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atLeastOnce, reset, verify, when}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.writeableOf_JsValue
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.{HttpReader, StatusHttpReader}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{EisHttpErrorHandler, EisHttpErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport // ✅ Ensure Json library is imported if needed

class EisHttpReaderSpec extends PlaySpec with GetRecordsDataSupport with EitherValues {

  val correlationId: String = "1234-456"

  class TestEisHttpReaderHandler extends EisHttpErrorHandler

  def successErrorHandler: (HttpResponse, String) => EisHttpErrorResponse =
    (_, correlationId) =>
      EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error"))

  "HttpReader" should {
    "handle error response" in {
      val errorHandlerSpy = mock[TestEisHttpReaderHandler]
      val eisResponse = HttpResponse(400, Json.toJson(getEisRecordsResponseData), Map.empty) // ✅ Ensure valid JSON response

      when(errorHandlerSpy.handleErrorResponse(any[HttpResponse], any[String]))
        .thenReturn(EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error")))

      HttpReader[GetEisRecordsResponse](correlationId, errorHandlerSpy.handleErrorResponse)
        .read("GET", "any-url", eisResponse)

      verify(errorHandlerSpy).handleErrorResponse(any[HttpResponse], any[String])
    }
  }

  "remove record responseHandler" should {
    "handle error response" in {
      val errorHandlerSpy = mock[TestEisHttpReaderHandler]
      val eisResponse = HttpResponse(400, "")

      when(errorHandlerSpy.handleErrorResponse(any[HttpResponse], any[String]))
        .thenReturn(EisHttpErrorResponse(INTERNAL_SERVER_ERROR, ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error")))

      StatusHttpReader(correlationId, errorHandlerSpy.handleErrorResponse)
        .read("GET", "any-url", eisResponse)

      verify(errorHandlerSpy).handleErrorResponse(any[HttpResponse], any[String])
    }
  }
}

