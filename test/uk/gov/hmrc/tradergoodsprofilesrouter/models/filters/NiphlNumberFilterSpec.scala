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

package uk.gov.hmrc.tradergoodsprofilesrouter.models.filters

import org.scalatestplus.play.PlaySpec

class NiphlNumberFilterSpec extends PlaySpec {

  object TestNiphlNumberFilter extends NiphlNumberFilter
  "removeLeadingDashed" should {
    "remove leading dashes" in {

      TestNiphlNumberFilter.removeLeadingDashes(Some("--124567")) mustBe Some("124567")

    }

    "do nothing id has not dashes" in {
      TestNiphlNumberFilter.removeLeadingDashes(Some("124567")) mustBe Some("124567")
    }

    "return None if niphlNumber is None" in {
      TestNiphlNumberFilter.removeLeadingDashes(None) mustBe None
    }
  }
}
