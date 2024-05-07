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

package uk.gov.hmrc.tradergoodsprofilesrouter

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.{Application, inject}
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.tradergoodsprofilesrouter.BaseIntegrationSpec.CustomMatchers.{haveJsonBody, haveNoBody, haveStatus}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{DateTimeService, UuidService}

abstract class BaseIntegrationSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with WireMockSupport {

  val wsClient: WSClient                      = app.injector.instanceOf[WSClient]
  val baseUrl: String                         = s"http://localhost:$port"
  lazy val uuidService: UuidService           = mock[UuidService]
  lazy val dateTimeService: DateTimeService   = mock[DateTimeService]
  override def fakeApplication(): Application =
    baseApplicationBuilder()
      .configure(extraApplicationConfig)
      .overrides(
        inject.bind[UuidService].toInstance(uuidService),
        inject.bind[DateTimeService].toInstance(dateTimeService)
      )
      .build()

  def extraApplicationConfig: Map[String, Any] = Map.empty

  def baseApplicationBuilder(): GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .configure(
        "metrics.enabled" -> false
      )

  def fullUrl(path: String): String = baseUrl + "/trader-goods-profiles-router" + path

  def assertAsExpected(response: WSResponse, status: Int, jsonBodyOpt: Option[String] = None): Unit = {
    response should haveStatus(status)
    jsonBodyOpt match {
      case Some(body) => response should haveJsonBody(body)
      case None       => response should haveNoBody
    }
    ()
  }

}

object BaseIntegrationSpec {

  object CustomMatchers {

    class HaveStatus(expectedStatus: Int) extends Matcher[WSResponse] {
      override def apply(response: WSResponse): MatchResult =
        MatchResult(
          response.status == expectedStatus,
          s"We expected the response to have status [$expectedStatus], but it was actually [${response.status}].",
          s"We didn't expect the response to have status [$expectedStatus], but it was indeed."
        )
    }

    class HaveJsonBody(expectedRawJsonBody: String) extends Matcher[WSResponse] {
      override def apply(response: WSResponse): MatchResult = {
        val expectedJsonBody = Json.parse(expectedRawJsonBody)
        val actualJsonBody   = Json.parse(response.body)
        MatchResult(
          actualJsonBody == expectedJsonBody,
          s"We expected the response to have a json body [\n${Json
            .prettyPrint(expectedJsonBody)}\n], but it was actually [\n${Json.prettyPrint(actualJsonBody)}\n].",
          s"We didn't expect the response to have a json body [\n${Json.prettyPrint(expectedJsonBody)}], but it was indeed."
        )
      }
    }

    class HaveNoBody() extends Matcher[WSResponse] {
      override def apply(response: WSResponse): MatchResult = {
        val actualBody = response.body
        MatchResult(
          actualBody == "",
          s"We expected the response to have no body, but it was actually [\n$actualBody\n].",
          s"We expected the response to have a body, but it didn't."
        )
      }
    }

    def haveStatus(expectedStatus: Int) = new HaveStatus(expectedStatus)

    def haveJsonBody(expectedRawJsonBody: String) = new HaveJsonBody(expectedRawJsonBody)

    def haveNoBody = new HaveNoBody()

  }
}
