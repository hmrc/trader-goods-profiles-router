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

  def verifyThatDownstreamApiWasNotCalled(): Unit =
    verify(0, postRequestedFor(urlEqualTo(connectorPath)))
}
