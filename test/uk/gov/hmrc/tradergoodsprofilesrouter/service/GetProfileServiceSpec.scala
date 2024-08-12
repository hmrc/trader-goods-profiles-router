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

package uk.gov.hmrc.tradergoodsprofilesrouter.service

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesrouter.connectors.{EisHttpErrorResponse, GetProfileConnector}
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.ProfileResponse
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.errors.ErrorResponse

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class GetProfileServiceSpec extends PlaySpec with EitherValues with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = new HeaderCarrier()

  private val eori = "123"
  private val correlationId = UUID.randomUUID().toString
  private val connector = mock[GetProfileConnector]
  private val uuidService = mock[UuidService]
  private val sut = new GetProfileService(connector, uuidService)


  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(connector, uuidService)
    when(connector.get(any, any)(any)).thenReturn(
      Future.successful(Right(ProfileResponse(eori, "123", None, None, None)))
    )
    when(uuidService.uuid).thenReturn(correlationId)
  }

  "getProfile" should {
    "return the profile information" in {
      val result = await(sut.getProfile(eori))

      result.value mustBe ProfileResponse(eori, "123", None, None, None)

      verify(connector).get(eqTo(eori), eqTo(correlationId))(any)
    }

    "return an error" in {
      when(connector.get(any, any)(any)).thenReturn(
        Future.successful(Left(EisHttpErrorResponse(500, ErrorResponse(correlationId, "code", "message", None))))
      )
      val result = await(sut.getProfile(eori))

      result.left.value mustBe EisHttpErrorResponse(500, ErrorResponse(correlationId, "code", "message", None))
    }

    "return a 500 if connector throw" in {
      when(connector.get(any, any)(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(sut.getProfile(eori))

      result.left.value mustBe EisHttpErrorResponse(
        500,
        ErrorResponse(correlationId, "UNEXPECTED_ERROR", "error")
      )
    }

  }
}
