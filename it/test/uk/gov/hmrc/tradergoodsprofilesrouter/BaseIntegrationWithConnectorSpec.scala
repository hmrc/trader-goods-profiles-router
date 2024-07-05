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

  def hawkConnectorPath: Option[String] = None
  def pegaConnectorPath: Option[String] = None

  def hawkConnectorName: String = "hawk"
  def pegaConnectorName: String = "pega"

  override def extraApplicationConfig: Map[String, Any] = {
    val hawkConfig = hawkConnectorPath
      .map(path =>
        Map(
          s"microservice.services.$hawkConnectorName.host" -> wireMockHost,
          s"microservice.services.$hawkConnectorName.port" -> wireMockPort,
          s"microservice.services.$hawkConnectorName.uri"  -> path,
          "auditing.enabled" -> false
        )
      )
      .getOrElse(Map.empty)

    val pegaConfig = pegaConnectorPath
      .map(path =>
        Map(
          s"microservice.services.$pegaConnectorName.host" -> wireMockHost,
          s"microservice.services.$pegaConnectorName.port" -> wireMockPort,
          s"microservice.services.$pegaConnectorName.uri"  -> path,
          "auditing.enabled" -> false
        )
      )
      .getOrElse(Map.empty)

    hawkConfig ++ pegaConfig
  }

  def verifyThatDownstreamApiWasCalled(connectorPath: Option[String]): Unit    =
    connectorPath.foreach { path =>
      withClue(s"We expected a single downstream API (stub) to be called at $path, but it wasn't.") {
        getAllServeEvents.asScala.count(_.getWasMatched) shouldBe 1
      }
    }
  def verifyThatMultipleDownstreamApiWasCalled(): Unit                         =
    withClue("We expected multiple downstream API (stub) to be called, but it wasn't.") {
      getAllServeEvents.asScala.count(_.getWasMatched) shouldBe 2
    }
  def verifyThatDownstreamApiWasNotCalled(connectorPath: Option[String]): Unit =
    connectorPath.foreach { path =>
      verify(0, postRequestedFor(urlEqualTo(path)))
    }
}
