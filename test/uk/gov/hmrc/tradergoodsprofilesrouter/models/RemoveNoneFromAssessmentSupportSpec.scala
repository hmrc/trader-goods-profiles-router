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

package uk.gov.hmrc.tradergoodsprofilesrouter.models

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tradergoodsprofilesrouter.models.RemoveNoneFromAssessmentSupport.removeEmptyAssessment
import uk.gov.hmrc.tradergoodsprofilesrouter.models.response.eis.{Assessment, Condition}

class RemoveNoneFromAssessmentSupportSpec extends PlaySpec {

  "remove none from assessment" should {
    "all none" in {
      removeEmptyAssessment(Some(Seq(Assessment(None, None, Some(Condition(None, None, None, None)))))) mustBe Some(
        Seq.empty
      )
    }
    "condition none" in {
      removeEmptyAssessment(Some(Seq(Assessment(None, None, None)))) mustBe Some(Seq.empty)
    }
    "all values present" in {
      val assessments = Some(
        Seq(Assessment(Some("123"), Some(1), Some(Condition(Some("type"), Some("1"), Some("desc"), Some("text")))))
      )
      removeEmptyAssessment(assessments) mustBe assessments
    }
  }
}
