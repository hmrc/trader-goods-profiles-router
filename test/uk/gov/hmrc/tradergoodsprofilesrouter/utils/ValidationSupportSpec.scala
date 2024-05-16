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

package uk.gov.hmrc.tradergoodsprofilesrouter.utils

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tradergoodsprofilesrouter.utils.ValidationSupport.{isValidCountryCode, isValidDate}

class ValidationSupportSpec extends PlaySpec {

  "when validating" should {
    "a valid date must return true" in {
      isValidDate("2024-11-18T23:20:19Z") mustBe true
    }
    "an invalid date must return false" in {
      isValidDate("2024-32-03") mustBe false
    }
    "a valid countryCode must return true" in {
      isValidCountryCode("GB") mustBe true
    }
    "an invalid countryCode must return false" in {
      isValidCountryCode("GB098765112") mustBe false
    }
  }
}
