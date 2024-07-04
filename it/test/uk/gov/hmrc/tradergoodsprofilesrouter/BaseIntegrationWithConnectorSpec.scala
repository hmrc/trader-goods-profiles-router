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

import com.github.tomakehurst.wiremock.client.WireMock.{getAllServeEvents, postRequestedFor, urlEqualTo, verify}

import scala.jdk.CollectionConverters.CollectionHasAsScala

abstract class BaseIntegrationWithConnectorSpec extends BaseIntegrationSpec {

  def connectorPath: String
  def connectorName: String

  override def extraApplicationConfig: Map[String, Any] = Map(
    s"microservice.services.$connectorName.host" -> wireMockHost,
    s"microservice.services.$connectorName.port" -> wireMockPort,
    s"microservice.services.$connectorName.uri"  -> connectorPath
  )

  def verifyThatDownstreamApiWasCalled(): Unit =
    withClue("We expected a single downstream API (stub) to be called, but it wasn't.") {
      getAllServeEvents.asScala.count(_.getWasMatched) shouldBe 1
    }
  def verifyThatMultipleDownstreamApiWasCalled(): Unit =
    withClue("We expected a multiple downstream API (stub) to be called, but it wasn't.") {
      getAllServeEvents.asScala.count(_.getWasMatched) shouldBe 2
    }
  def verifyThatDownstreamApiWasNotCalled(): Unit =
    verify(0, postRequestedFor(urlEqualTo(connectorPath)))
}
