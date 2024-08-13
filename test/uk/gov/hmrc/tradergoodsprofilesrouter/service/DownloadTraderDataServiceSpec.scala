package uk.gov.hmrc.tradergoodsprofilesrouter.service

import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{DownloadTraderDataConnector, EisHttpErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.support.CreateRecordDataSupport

import scala.concurrent.{ExecutionContext, Future}

class DownloadTraderDataServiceSpec
    extends PlaySpec
    with CreateRecordDataSupport
    with EitherValues
    with ScalaFutures
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val eori          = "GB123456789011"
  private val connector     = mock[DownloadTraderDataConnector]
  private val uuidService   = mock[UuidService]
  private val correlationId = "1234-5678-9012"

  private val service = new DownloadTraderDataService(connector, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(connector, uuidService)
    when(uuidService.uuid).thenReturn(correlationId)
  }

  "download trader data" should {
    "return ACCEPTED when the request is accepted by EIS" in {
      when(connector.requestDownload(eori, correlationId)).thenReturn(Future.successful(Right(ACCEPTED)))

      val result = await(service.requestDownload(eori))

      result.value mustBe ACCEPTED
    }

    "return any error that is returned by EIS" in {
      val errorResponse = EisHttpErrorResponse(
        BAD_REQUEST,
        ErrorResponse(
          correlationId,
          "BAD_REQUEST",
          "BAD_REQUEST"
        )
      )

      when(connector.requestDownload(eori, correlationId)).thenReturn(Future.successful(Left(errorResponse)))

      val result = await(service.requestDownload(eori))

      result.left.value mustBe errorResponse
    }

    "returns an internal server error when an exception is thrown by EIS" in {
      when(connector.requestDownload(eori, correlationId))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(service.requestDownload(eori))

      result.left.value mustBe EisHttpErrorResponse(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error")
      )
    }
  }
}
