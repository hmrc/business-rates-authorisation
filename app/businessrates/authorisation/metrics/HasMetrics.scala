/*
 * Copyright 2017 HM Revenue & Customs
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

package businessrates.authorisation.metrics

import com.codahale.metrics._
import com.kenshoo.play.metrics.Metrics

trait HasMetrics {

  type Metric = String

  val metrics: Metrics

  val localMetrics = new LocalMetrics

  lazy val registry: MetricRegistry = metrics.defaultRegistry

  class MetricsTimer(metric: Metric) {
    val timer: Timer.Context = localMetrics.startTimer(metric)

    def completeTimerAndMarkAsSuccess(): Unit = {
      timer.stop()
      localMetrics.incrementSuccessCounter(metric)
      localMetrics.incrementSuccessMeter(metric)
    }

    def completeTimerAndMarkAsFailure(): Unit = {
      timer.stop()
      localMetrics.incrementFailedCounter(metric)
      localMetrics.incrementFailedMeter(metric)
    }
  }

  def withMetricsTimer[T](metric: Metric)(block: MetricsTimer => T): T =
    block(new MetricsTimer(metric))

  class LocalMetrics {
    def startTimer(metric: Metric): Timer.Context = registry.timer(s"$metric/timer").time()

    def stopTimer(context: Timer.Context): Long = context.stop()

    def incrementSuccessMeter(metric: Metric): Unit = registry.meter(s"$metric/success-meter").mark()

    def incrementFailedMeter(metric: Metric): Unit = registry.meter(s"$metric/failed-meter").mark()

    def incrementSuccessCounter(metric: Metric): Unit = registry.counter(s"$metric/success-counter").inc()

    def incrementFailedCounter(metric: Metric): Unit = registry.counter(s"$metric/failed-counter").inc()
  }

}
