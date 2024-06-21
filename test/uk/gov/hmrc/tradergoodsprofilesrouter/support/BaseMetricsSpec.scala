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

package uk.gov.hmrc.tradergoodsprofilesrouter.support

import com.codahale.metrics.{Counter, MetricRegistry, Timer}
import org.mockito.ArgumentMatchers.endsWith
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.MockitoSugar.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock

trait BaseMetricsSpec {

  val metricsRegistry: MetricRegistry = mock[MetricRegistry](RETURNS_DEEP_STUBS)
  val timerContext                    = mock[Timer.Context]
  private val successCounter          = mock[Counter]
  private val failureCounter          = mock[Counter]

  protected def setUpMetrics(): Unit = {
    when(metricsRegistry.counter(endsWith("success-counter"))) thenReturn successCounter
    when(metricsRegistry.counter(endsWith("failed-counter"))) thenReturn failureCounter
    when(metricsRegistry.timer(any).time()) thenReturn timerContext
    when(timerContext.stop()) thenReturn 0L
  }

  protected def verifyMetrics(timerName: String) = {
    verify(metricsRegistry).timer(eqTo(s"$timerName-timer"))
    verify(timerContext).stop()
  }
}
