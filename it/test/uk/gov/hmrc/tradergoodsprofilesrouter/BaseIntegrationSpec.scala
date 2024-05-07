package uk.gov.hmrc.tradergoodsprofilesrouter

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.{Application, inject}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.tradergoodsprofilesrouter.BaseIntegrationSpec.CustomMatchers.{haveJsonBody, haveNoBody, haveStatus}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{DateTimeService, UuidService}

import java.time.Clock

abstract class BaseIntegrationSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with WireMockSupport {

  val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val baseUrl: String    = s"http://localhost:$port"
  lazy val uuidService: UuidService = mock[UuidService]
  lazy val dateTimeService: DateTimeService = mock[DateTimeService]
  override def fakeApplication(): Application =
    baseApplicationBuilder()
      .configure(extraApplicationConfig)
      .overrides(inject.bind[UuidService].toInstance(uuidService))
      .overrides(inject.bind[DateTimeService].toInstance(dateTimeService))
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
