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
import org.mockito.MockitoSugar.verify
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MetricsSupportSpec extends PlaySpec {

  implicit val ec: ExecutionContext = ExecutionContext.global

  trait MockHasMetrics {
    self: MetricsSupport =>
    val timerContext                             = mock[Timer.Context]
    val timer                                    = mock[Timer]
    val successCounter                           = mock[Counter]
    val failureCounter                           = mock[Counter]
    val histogram                                = mock[Histogram]
    override val metricsRegistry: MetricRegistry = mock[MetricRegistry]
    when(this.metricsRegistry.timer(anyString())) thenReturn timer
    when(this.metricsRegistry.counter(endsWith("success-counter"))) thenReturn successCounter
    when(this.metricsRegistry.counter(endsWith("failed-counter"))) thenReturn failureCounter
    when(this.metricsRegistry.histogram(anyString())) thenReturn histogram
    when(timer.time()) thenReturn timerContext
    when(timerContext.stop()) thenReturn 0L
  }

  private val metricName = "test-metrics"

  class TestMetricsSupport @Inject() extends MetricsSupport with MockHasMetrics

  def withTestMetrics[A](test: TestMetricsSupport => A): A =
    test(new TestMetricsSupport())

  "withMetricsTimerAsync" should {
    "start a timer and increment a counter for a successful future" in withTestMetrics { metrics =>
      await(metrics.withMetricsTimerAsync(metricName)(_ => Future.successful(())))

      verifyCompletedWithSuccess(metricName, metrics)
    }

    "increment success counter for a successful future where completeWithSuccess is called explicitly" in withTestMetrics {
      metrics =>
        await(metrics.withMetricsTimerAsync(metricName) { timer =>
          timer.completeWithSuccess()
          Future.successful(())
        })

        verifyCompletedWithSuccess(metricName, metrics)
    }

    "increment failure counter for a failed future" in withTestMetrics { metrics =>
      intercept[Exception] {
        await(metrics.withMetricsTimerAsync(metricName)(_ => Future.failed(new Exception)))

        verifyCompletedWithFailure(metricName, metrics)
      }

    }

    "increment failure counter for a successful future where completeWithFailure is called explicitly" in withTestMetrics {
      metrics =>
        await(metrics.withMetricsTimerAsync(metricName) { timer =>
          timer.completeWithFailure()
          Future.successful(())
        })

        verifyCompletedWithFailure(metricName, metrics)
    }

    "only increment counters once regardless of how many times the user calls complete with success" in withTestMetrics {
      metrics =>
        await(
          metrics
            .withMetricsTimerAsync(metricName) { timer =>
              Future(timer.completeWithSuccess())
              Future(timer.completeWithSuccess())
              Future(timer.completeWithSuccess())
              Future(timer.completeWithSuccess())
              Future.successful(())
            }
        )

        verifyCompletedWithSuccess(metricName, metrics)
    }

    "only increment counters once regardless of how many times the user calls complete with failure" in withTestMetrics {
      metrics =>
        await(
          metrics
            .withMetricsTimerAsync(metricName) { timer =>
              Future(timer.completeWithFailure())
              Future(timer.completeWithFailure())
              Future(timer.completeWithFailure())
              Future(timer.completeWithFailure())
              timer.completeWithFailure()
              Future.successful(())
            }
        )

        verifyCompletedWithFailure(metricName, metrics)
    }

    "increment failure counter when the user throws an exception constructing their code block" in withTestMetrics {
      metrics =>
        assertThrows[RuntimeException] {
          await(metrics.withMetricsTimerAsync(metricName)(_ => Future.successful(throw new RuntimeException)))
        }

        verifyCompletedWithFailure(metricName, metrics)
    }

  }

  def verifyCompletedWithSuccess(metricName: String, metrics: MockHasMetrics) = {
    verify(metrics.metricsRegistry).timer(s"$metricName-timer")
    val inOrder = Mockito.inOrder(metrics.timer, metrics.timerContext, metrics.successCounter)
    inOrder.verify(metrics.timer, times(1)).time()
    inOrder.verify(metrics.timerContext, times(1)).stop()
    inOrder.verify(metrics.successCounter, times(1)).inc()
    verifyNoMoreInteractions(metrics.timer)
    verifyNoMoreInteractions(metrics.timerContext)
    verifyNoMoreInteractions(metrics.successCounter)
    verifyNoInteractions(metrics.failureCounter)
  }

  def verifyCompletedWithFailure(metricName: String, metrics: MockHasMetrics) = {
    verify(metrics.metricsRegistry).timer(s"$metricName-timer")
    val inOrder = Mockito.inOrder(metrics.timer, metrics.timerContext, metrics.failureCounter)
    inOrder.verify(metrics.timer, times(1)).time()
    inOrder.verify(metrics.timerContext, times(1)).stop()
    inOrder.verify(metrics.failureCounter, times(1)).inc()
    verifyNoMoreInteractions(metrics.timer)
    verifyNoMoreInteractions(metrics.timerContext)
    verifyNoMoreInteractions(metrics.failureCounter)
    verifyNoInteractions(metrics.successCounter)
  }
}
