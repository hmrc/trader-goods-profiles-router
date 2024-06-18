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

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{doReturn, spy, verify}
import org.scalatest.EitherValues
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EisHttpReader.{LegacyHttpReader, LegacyStatusHttpReader}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.GetEisRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.GetRecordsDataSupport

import scala.reflect.runtime.universe.typeOf

class EisHttpReaderSpec extends PlaySpec with GetRecordsDataSupport with EitherValues {

  val correlationId: String = "1234-456"

  class TestEisHttpReaderHandler extends LegacyEisHttpErrorHandler
  def successErrorHandler: (HttpResponse, String) => Result = (response, correlationId) => Ok("Success")

  "LegacyHttpReader" should {
    "return a record item" in {
      val eisResponse = HttpResponse(200, getEisRecordsResponseData, Map.empty)

      val result =
        LegacyHttpReader[GetEisRecordsResponse](correlationId, successErrorHandler).read("GET", "any-url", eisResponse)

      result.value mustBe getEisRecordsResponseData.as[GetEisRecordsResponse]
    }

    "handle error response" in {
      val errorHandlerSpy = spyOnHandlerErrorFn
      val eisResponse     = HttpResponse(400, getEisRecordsResponseData, Map.empty)

      LegacyHttpReader[GetEisRecordsResponse](correlationId, errorHandlerSpy.legacyHandleErrorResponse)
        .read("GET", "any-url", eisResponse)

      verify(errorHandlerSpy).legacyHandleErrorResponse(eisResponse, correlationId)
    }

    "throw an error if cannot parse the response as json" in {
      val eisResponse = HttpResponse(200, "message")

      the[RuntimeException] thrownBy {
        LegacyHttpReader[GetEisRecordsResponse](correlationId, successErrorHandler).read("GET", "any-url", eisResponse)
      } must have message "Response body could not be read: message"
    }

    "throw an error if cannot parse the response as Object" in {
      val eisResponse = HttpResponse(200, """{"eori": "GB1234567890"}""")

      the[RuntimeException] thrownBy {
        LegacyHttpReader[GetEisRecordsResponse](correlationId, successErrorHandler)
          .read("GET", "any-url", eisResponse)
      } must have message s"Response body could not be read as type ${typeOf[GetEisRecordsResponse]}"
    }
  }

  "remove record responseHandler" should {
    "remove a record item" in {

      val eisResponse = HttpResponse(200, "")
      val result      = LegacyStatusHttpReader(correlationId, successErrorHandler).read("PUT", "any-url", eisResponse)

      result.value mustBe OK
    }

    "handle error response" in {
      val eisResponse     = HttpResponse(400, "")
      val errorHandlerSpy = spyOnHandlerErrorFn

      LegacyStatusHttpReader(correlationId, errorHandlerSpy.legacyHandleErrorResponse)
        .read("GET", "any-url", eisResponse)

      verify(errorHandlerSpy).legacyHandleErrorResponse(eisResponse, correlationId)
    }

  }

  private def spyOnHandlerErrorFn = {
    val errorHandlerSpy = spy(new TestEisHttpReaderHandler)
    doReturn(Ok("Success")).when(errorHandlerSpy).legacyHandleErrorResponse(any, any)
    errorHandlerSpy
  }

}
