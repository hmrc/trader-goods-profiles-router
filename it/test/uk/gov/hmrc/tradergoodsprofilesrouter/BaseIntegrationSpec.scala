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
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.{Application, inject}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{DateTimeService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.AuthTestSupport

abstract class BaseIntegrationSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with AuthTestSupport
    with WireMockSupport {

  val wsClient: WSClient                      = app.injector.instanceOf[WSClient]
  val baseUrl: String                         = s"http://localhost:$port"
  lazy val uuidService: UuidService           = mock[UuidService]
  lazy val dateTimeService: DateTimeService   = mock[DateTimeService]
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
}
