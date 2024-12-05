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

package businessrates.authorisation.metrics

import com.codahale.metrics._
import play.api.libs.json.Writes
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

trait HasMetrics extends DefaultHttpClient {

  type Metric = String

  val metrics: Metrics

  lazy val registry: MetricRegistry = metrics.defaultRegistry

  override def doPatch[A](url: String, body: A, headers: Seq[(String, String)])(implicit
        rds: Writes[A],
        ec: ExecutionContext
  ): Future[HttpResponse] =
    withMetricsTimer(getApiName(url))(super.doPatch(url, body, headers))

  override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit
        rds: Writes[A],
        ec: ExecutionContext
  ): Future[HttpResponse] =
    withMetricsTimer(getApiName(url))(super.doPost(url, body, headers))

  override def doDelete(url: String, headers: Seq[(String, String)])(implicit
        ec: ExecutionContext
  ): Future[HttpResponse] =
    withMetricsTimer(getApiName(url))(super.doDelete(url, headers))

  override def doPut[A](url: String, body: A, headers: Seq[(String, String)])(implicit
        rds: Writes[A],
        ec: ExecutionContext
  ): Future[HttpResponse] =
    withMetricsTimer(getApiName(url))(super.doPut(url, body, headers))

  override def doGet(url: String, headers: Seq[(String, String)])(implicit ec: ExecutionContext): Future[HttpResponse] =
    withMetricsTimer(getApiName(url))(super.doGet(url, headers))

  private def getApiName(url: String): String = new URL(url).getPath.drop(1).split("/").head

  private def withMetricsTimer(
        metric: Metric
  )(block: => Future[HttpResponse])(implicit executionContext: ExecutionContext): Future[HttpResponse] = {
    val timer = MetricsTimer(metric)
    block.map { response =>
      timer.complete(response.status)
      response
    } recover {
      case ex: Exception =>
        timer.fail()
        throw ex
    }
  }

  case class MetricsTimer(metric: Metric) {
    private val timer = registry.timer(s"$metric/timer").time()

    def complete: Int => Unit = {
      case s if s >= 200 && s <= 399 => success()
      case _                         => fail()
    }

    def success(): Unit = op("success")

    def fail(): Unit = op("failed")

    private def op(status: String): Unit = {
      timer.stop()
      registry.counter(s"$metric/$status-counter").inc()
      registry.meter(s"$metric/$status-meter").mark()
    }
  }
}
