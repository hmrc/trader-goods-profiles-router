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

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tradergoodsprofilesrouter.service.DateTimeService.DateTimeFormat

import java.time.Instant

class DateTimeServiceSpec extends PlaySpec {

  "asStringHttp" should {
    "return date and time in specified format" in {
      val dateTime = Instant.parse("2021-12-17T09:30:47Z")
      dateTime.asStringHttp mustBe "Fri, 17 Dec 2021 09:30:47 Z"
    }
  }

}
