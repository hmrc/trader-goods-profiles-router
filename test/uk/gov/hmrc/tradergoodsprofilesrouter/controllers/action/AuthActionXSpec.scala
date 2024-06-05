package uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{FORBIDDEN, INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.mvc.{BodyParsers, Request, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsJson, defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.{Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService
import uk.gov.hmrc.tradergoodsprofilesrouter.support.AuthTestSupport

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AuthActionXSpec extends PlaySpec with AuthTestSupport with ScalaFutures with BeforeAndAfterEach {

  implicit private val ec: ExecutionContext = ExecutionContext.global

  private val correlationId = UUID.randomUUID().toString
  private val uuidService   = mock[UuidService]
  private val bodyParsers   = mock[BodyParsers.Default]

  private val sut = new AuthActionX(
    authConnector,
    uuidService,
    stubMessagesControllerComponents()
  )

  def block(request: Request[_]): Future[Result] =
    Future.successful(Results.Ok)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(authConnector, uuidService)

    when(uuidService.uuid).thenReturn(correlationId)
  }
  "authorise" should {
    "authorised" when {
      "enrolment is valid" in {
        withAuthorizedTrader()

        val result = sut.authorisedActionWithEori(bodyParsers, eoriNumber)(block).apply(FakeRequest())

        status(result) mustBe OK
        verify(authConnector).authorise(eqTo(Enrolment("HMRC-CUS-ORG")), any)(any, any)
      }
    }

    "return an error" when {
      "enrolment is invalid" in {
        withUnauthorizedTrader(InsufficientEnrolments())

        val result = sut.authorisedActionWithEori(bodyParsers, eoriNumber)(block).apply(FakeRequest())

        status(result) mustBe UNAUTHORIZED
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "UNAUTHORIZED",
          "message"       -> "Downstream error: The details signed in do not have a Trader Goods Profile"
        )
      }

      "return internal server error when throwing" in {

        withUnauthorizedTrader(new RuntimeException("unauthorised error"))

        val result = sut.authorisedActionWithEori(bodyParsers, eoriNumber)(block).apply(FakeRequest(GET, "/get"))

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "INTERNAL_SERVER_ERROR",
          "message"       -> "Downstream error: Internal server error for /get with error: unauthorised error"
        )
      }

      "identifier is not found" in {
        withUnauthorizedEmptyIdentifier()

        val result = sut.authorisedActionWithEori(bodyParsers, eoriNumber)(block).apply(FakeRequest())

        status(result) mustBe FORBIDDEN
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "FORBIDDEN",
          "message"       -> "Downstream error: EORI number is incorrect"
        )
      }

      "return forbidden if identifier is unauthorized" in {
        withAuthorizedTrader()

        val result = sut.authorisedActionWithEori(bodyParsers, "any-roi")(block).apply(FakeRequest())

        status(result) mustBe FORBIDDEN
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "FORBIDDEN",
          "message"       -> s"Downstream error: EORI number is incorrect"
        )
      }
    }
  }
}
