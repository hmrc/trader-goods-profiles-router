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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{GET, contentAsJson, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector
import org.mockito.ArgumentMatchersSugar.eqTo
import play.api.http.MimeTypes
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.HeaderNames

import java.time.{OffsetDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.concurrent.Future
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class GetRecordsControllerSpec extends PlaySpec with MockitoSugar {

  "GetRecordsController GET /:eori/record/:recordId" should {

    "return a successful JSON response for a single record" in {
      val mockEisConnector = mock[EISConnector]
      val controller       = GetRecordsController(
        Helpers.stubControllerComponents(),
        mockEisConnector
      )

      val eori                = "GB123456789011"
      val recordId            = "12345"

      val HTTP_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH).withZone(ZoneOffset.UTC)

      val expectedJson        = Json.obj(
        "message"  -> "EIS record retrieved successfully",
        "eori"     -> eori,
        "recordId" -> recordId
      )

      val headers = Seq(
        HeaderNames.CORRELATION_ID -> "3e8dae97-b586-4cef-8511-68ac12da9028",
        HeaderNames.FORWARDED_HOST -> "0.0.0.0",
        HeaderNames.CONTENT_TYPE   -> MimeTypes.JSON,
        HeaderNames.ACCEPT         -> MimeTypes.JSON,
        HeaderNames.DATE           -> HTTP_DATE_FORMATTER.format(OffsetDateTime.now()),
        HeaderNames.CLIENT_ID      -> "clientId",
        HeaderNames.AUTHORIZATION  -> "bearerToken"
      )

      val mockHttpResponse = HttpResponse(200, expectedJson.toString())


      val fakeRequest                = FakeRequest(GET, s"/$eori/record/$recordId")
        .withHeaders(headers: _*)

      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(
        mockEisConnector.fetchRecord(
          eqTo(eori),
          eqTo(recordId)
        )(any, any)
      )
        .thenReturn(Future.successful(mockHttpResponse))

      val result = controller
        .getTGPRecord(eori, recordId)
        .apply(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe expectedJson
    }
  }
}
