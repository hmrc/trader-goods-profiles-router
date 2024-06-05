package uk.gov.hmrc.tradergoodsprofilesrouter.controllers.action

import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.mvc.BodyParsers
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.tradergoodsprofilesrouter.service.UuidService

import java.util.UUID
import scala.concurrent.ExecutionContext

class ValidateRequestBodyActionSpec extends PlaySpec {

  implicit private val ec: ExecutionContext = ExecutionContext.global

  private val correlationId = UUID.randomUUID().toString
  private val uuidService   = mock[UuidService]

  private val sut = new ValidateRequestBodyAction(
    mock[BodyParsers.Default],
    uuidService,
    stubMessagesControllerComponents()
  )

}
