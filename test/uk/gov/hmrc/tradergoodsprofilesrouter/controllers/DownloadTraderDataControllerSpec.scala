package uk.gov.hmrc.tradergoodsprofilesrouter.controllers

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.ACCEPTED
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofilesrouter.service.{DownloadTraderDataService, UuidService}
import uk.gov.hmrc.tradergoodsprofilesrouter.support.FakeAuth.FakeSuccessAuthAction

import scala.concurrent.{ExecutionContext, Future}

class DownloadTraderDataControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockService     = mock[DownloadTraderDataService]
  private val mockUuidService = mock[UuidService]

  private val eori          = "GB123456789011"
  private val correlationId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  private val controller =
    new DownloadTraderDataController(
      new FakeSuccessAuthAction(),
      mockService,
      stubControllerComponents(),
      mockUuidService
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockUuidService.uuid).thenReturn(correlationId)
  }

  "download trader profile" should {
    "return a 202 accepted response if the request is accepted by EIS" in {
      when(mockService.requestDownload(any)(any)).thenReturn(Future.successful(Right(202)))

      val result = controller.requestDataDownload(eori)(FakeRequest())
      status(result) mustBe ACCEPTED
    }
  }

}
