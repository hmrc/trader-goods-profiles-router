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

package uk.gov.hmrc.tradergoodsprofilesrouter.support

import com.github.tomakehurst.wiremock.client.WireMock.{getAllServeEvents, postRequestedFor, urlEqualTo, verify}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.{Application, inject}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{DateTimeService, UuidService}

import scala.jdk.CollectionConverters.CollectionHasAsScala

abstract class BaseIntegrationSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with AuthTestSupport
    with WireMockSupport {

  val wsClient: WSClient                    = app.injector.instanceOf[WSClient]
  val baseUrl: String                       = s"http://localhost:$port"
  lazy val uuidService: UuidService         = mock[UuidService]
  lazy val dateTimeService: DateTimeService = mock[DateTimeService]

  override def fakeApplication(): Application =
    baseApplicationBuilder()
      .configure(extraApplicationConfig)
      .overrides(
        inject.bind[AuthConnector].toInstance(authConnector),
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

  def fullUrl(path: String): String = s"$baseUrl/trader-goods-profiles-router$path"

  def verifyThatDownstreamApiWasCalled(connectorPath: String): Unit =
    withClue(s"We expected a single downstream API (stub) to be called at $connectorPath, but it wasn't.") {
      getAllServeEvents.asScala.count(_.getWasMatched) shouldBe 1
    }

  def verifyThatMultipleDownstreamApiWasCalled(): Unit                 =
    withClue("We expected multiple downstream API (stub) to be called, but it wasn't.") {
      getAllServeEvents.asScala.count(_.getWasMatched) shouldBe 2
    }
  def verifyThatDownstreamApiWasNotCalled(connectorPath: String): Unit =
    verify(0, postRequestedFor(urlEqualTo(connectorPath)))

  def verifyThatDownstreamApiWasRetried(connectorPath: String): Unit =
    withClue(s"We expected a downstream API (stub) to be retried at $connectorPath, but it wasn't.") {
      val event = getAllServeEvents.asScala.count(_.getWasMatched)
      event > 1 shouldBe true
    }
}
