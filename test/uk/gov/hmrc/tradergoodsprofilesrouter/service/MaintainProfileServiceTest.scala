package uk.gov.hmrc.tradergoodsprofilesrouter.service

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.EISConnector
import uk.gov.hmrc.tradergoodsprofilesrouter.models.request.MaintainProfileRequest
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.MaintainProfileResponse

import scala.concurrent.{ExecutionContext, Future}

class MaintainProfileServiceTest
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with EitherValues
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val eoriNumber    = "GB123456789011"
  private val actorId       = "GB123456789011"
  private val correlationId = "1234-5678-9012"
  private val eisConnector  = mock[EISConnector]
  private val uuidService   = mock[UuidService]

  val service = new MaintainProfileService(eisConnector, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(eisConnector, uuidService)
    when(uuidService.uuid).thenReturn(correlationId)
  }

  "maintain profile" should {
    "successfully maintain a profile" in {
      when(eisConnector.maintainProfile(any, any)(any))
        .thenReturn(Future.successful(Right(maintainProfileResponse)))

      val result = service.maintainProfile(maintainProfileRequest)

      whenReady(result.value) {
        _.value shouldBe maintainProfileResponse
      }
    }

    "return an bad request when EIS return a bad request" in {
      when(eisConnector.maintainProfile(any, any)(any))
        .thenReturn(Future.successful(Left(BadRequest("error"))))

      val result = service.maintainProfile(maintainProfileRequest)

      whenReady(result.value) {
        _.left.value shouldBe BadRequest("error")
      }
    }

    "return an internal server error when EIS returns one" in {
      when(eisConnector.maintainProfile(any, any)(any))
        .thenReturn(Future.failed(new RuntimeException("run time exception")))

      val result = service.maintainProfile(maintainProfileRequest)

      whenReady(result.value) {
        _.left.value shouldBe InternalServerError(
          Json.obj(
            "correlationId" -> correlationId,
            "code"          -> "UNEXPECTED_ERROR",
            "message"       -> "run time exception"
          )
        )
      }
    }

    lazy val maintainProfileRequest: MaintainProfileRequest =
      Json
        .parse("""
            |{
            |"eori": "GB123456789012",
            |"actorId":"GB098765432112",
            |"ukimsNumber":"XIUKIM47699357400020231115081800",
            |"nirmsNumber":"RMS-GB-123456",
            |"niphlNumber": "6S123456"
            |}
            |""".stripMargin)
        .as[MaintainProfileRequest]

    lazy val maintainProfileResponse: MaintainProfileResponse =
      Json
        .parse("""
            |{
            |"eori": "GB123456789012",
            |"actorId":"GB098765432112",
            |"ukimsNumber":"XIUKIM47699357400020231115081800",
            |"nirmsNumber":"RMS-GB-123456",
            |"niphlNumber": "6S123456"
            |}
            |""".stripMargin)
        .as[MaintainProfileResponse]

  }
}
