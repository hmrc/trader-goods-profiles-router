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

package uk.gov.hmrc.tradergoodsprofilesrouter.connectors.metrics

import com.codahale.metrics.{Counter, Histogram, MetricRegistry, Timer}
import org.mockito.ArgumentMatchers.{anyString, endsWith}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verifyNoInteractions, verifyNoMoreInteractions, when}
import org.scalatest.compatible.Assertion
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MetricsUtils1Spec extends AsyncFlatSpec {

  implicit val ec: ExecutionContext = ExecutionContext.global

  trait MockHasMetrics {
    self: MetricsUtils =>
    val timerContext   = mock[Timer.Context]
    val timer          = mock[Timer]
    val successCounter = mock[Counter]
    val failureCounter = mock[Counter]
    val histogram      = mock[Histogram]
    when(this.metricsRegistry.timer(anyString())) thenReturn timer
    when(this.metricsRegistry.counter(endsWith("success-counter"))) thenReturn successCounter
    when(this.metricsRegistry.counter(endsWith("failed-counter"))) thenReturn failureCounter
    when(this.metricsRegistry.histogram(anyString())) thenReturn histogram
    when(timer.time()) thenReturn timerContext
    when(timerContext.stop()) thenReturn 0L
  }

  private lazy val metricsRegistry: MetricRegistry = mock[MetricRegistry]
  private val metricName                           = "test-metrics"

  class TestMetricsSupport @Inject() (override val metricsRegistry: MetricRegistry)
      extends MetricsUtils
      with MockHasMetrics

  def withTestMetrics[A](test: TestMetricsSupport => A): A =
    test(new TestMetricsSupport(metricsRegistry))

  behavior of "withMetricsTimerAsync"

  "withMetricsTimerAsync" should
    "start a timer and increment a counter for a successful future" in {

      val metrics = new TestMetricsSupport(metricsRegistry)
      val result  = await(metrics.withMetricsTimerAsync(metricName) { timer =>
        Future(timer.completeWithFailure())
        Future(timer.completeWithFailure())
        Future(timer.completeWithFailure())
        Future(timer.completeWithFailure())
        timer.completeWithFailure()
        Future.successful(())
      })

      val inOrder = Mockito.inOrder(metrics.timer, metrics.timerContext, metrics.failureCounter)
      inOrder.verify(metrics.timer, times(1)).time()
      inOrder.verify(metrics.timerContext, times(1)).stop()
      inOrder.verify(metrics.failureCounter, times(1)).inc()
      verifyNoMoreInteractions(metrics.timer)
      verifyNoMoreInteractions(metrics.timerContext)
      verifyNoMoreInteractions(metrics.failureCounter)
      verifyNoInteractions(metrics.successCounter)
      succeed
    }

  def verifyCompletedWithSuccess(metricName: String, metrics: MockHasMetrics): Assertion = {
    val inOrder = Mockito.inOrder(metrics.timer, metrics.timerContext, metrics.successCounter)
    inOrder.verify(metrics.timer, times(1)).time()
    inOrder.verify(metrics.timerContext, times(1)).stop()
    inOrder.verify(metrics.successCounter, times(1)).inc()
    verifyNoMoreInteractions(metrics.timer)
    verifyNoMoreInteractions(metrics.timerContext)
    verifyNoMoreInteractions(metrics.successCounter)
    verifyNoInteractions(metrics.failureCounter)
    succeed
  }

  def verifyCompletedWithFailure(metricName: String, metrics: MockHasMetrics): Assertion = {
    val inOrder = Mockito.inOrder(metrics.timer, metrics.timerContext, metrics.failureCounter)
    inOrder.verify(metrics.timer, times(1)).time()
    inOrder.verify(metrics.timerContext, times(1)).stop()
    inOrder.verify(metrics.failureCounter, times(1)).inc()
    verifyNoMoreInteractions(metrics.timer)
    verifyNoMoreInteractions(metrics.timerContext)
    verifyNoMoreInteractions(metrics.failureCounter)
    verifyNoInteractions(metrics.successCounter)
    succeed
  }
}
