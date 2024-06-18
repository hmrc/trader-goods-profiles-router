package uk.gov.hmrc.tradergoodsprofilesrouter.service

import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.tradergoodsprofilesrouter.factories.AuditEventFactory

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class AuditServiceSpec extends PlaySpec with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders = Seq(("X-Client-ID", "TSS")))

  private val auditConnector    = mock[AuditConnector]
  private val auditEventFactory = mock[AuditEventFactory]
  private val auditSource       = "trader-goods-profiles-router"
  private val auditType         = "ManageGoodsRecord"
  private val dateTime          = Instant.parse("2021-12-17T09:30:47Z").toString
  private val eori              = "GB123456789011"
  private val recordId          = "d677693e-9981-4ee3-8574-654981ebe606"
  private val actorId           = "GB123456789011"

  val sut = new AuditService(auditConnector, auditEventFactory)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(auditConnector, auditEventFactory)
  }

  "auditRemoveRecord" should {
    "send an event for success response" in {

      when(auditEventFactory.createRemoveRecord(any, any, any, any)(any)).thenReturn(extendedSuccessDataEvent)
      when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(Success))

      val result = await(sut.auditRemoveRecord(eori, recordId, actorId, dateTime))

      result mustBe Done
      verify(auditConnector).sendExtendedEvent(extendedSuccessDataEvent)
    }
  }

  private val extendedSuccessDataEvent = ExtendedDataEvent(
    auditSource = auditSource,
    auditType = auditType,
    tags = hc.toAuditTags(),
    detail = auditDetails
  )

  val auditDetails =
    Json.obj(
      "journey"          -> "RemoveRecord",
      "clientId"         -> hc.headers(Seq("X-Client-ID")).head._2,
      "requestDateTime"  -> dateTime,
      "responseDateTime" -> dateTime,
      "request"          -> Json.obj(
        "eori"     -> eori,
        "recordId" -> recordId,
        "actorId"  -> actorId
      ),
      "outcome"          -> Json.obj(
        "status"     -> "SUCCEEDED",
        "statusCode" -> "204"
      )
    )
}
