/*
 * Copyright 2020 HM Revenue & Customs
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

package businessrates.authorisation.connectors

import akka.actor.ActorSystem
import businessrates.authorisation.metrics.HasMetrics
import com.kenshoo.play.metrics.Metrics
import com.typesafe.config.Config
import javax.inject.Inject
import play.api.Configuration
import play.api.libs.json.Writes
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.ws._

import scala.concurrent.{ExecutionContext, Future}

class VOABackendWSHttp @Inject()(
      config: Configuration,
      override val metrics: Metrics,
      override val auditConnector: AuditConnector,
      override val wsClient: WSClient,
      override val actorSystem: ActorSystem)
    extends HttpGet with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpPatch with WSPatch
    with HttpDelete with WSDelete with HasMetrics with HttpAuditing {

  override val hooks: Seq[HttpHook] = Seq(AuditingHook)

  override protected def configuration: Option[Config] = Some(config.underlying)

  override def appName: String = "business-rates-authorisation"

  def buildHeaderCarrier(hc: HeaderCarrier): HeaderCarrier =
    HeaderCarrier(requestId = hc.requestId, sessionId = hc.sessionId)
      .withExtraHeaders(hc.extraHeaders: _*)

  override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(
        implicit rds: Writes[A],
        hc: HeaderCarrier,
        ec: ExecutionContext): Future[HttpResponse] =
    super.doPost(url, body, headers)(rds, buildHeaderCarrier(hc), ec)

  override def doDelete(url: String, headers: Seq[(String, String)])(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[HttpResponse] =
    super.doDelete(url, headers)(buildHeaderCarrier(hc), ec)

  override def doGet(url: String, headers: Seq[(String, String)])(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[HttpResponse] =
    super.doGet(url, headers)(buildHeaderCarrier(hc), ec)

  override def doPut[A](url: String, body: A, headers: Seq[(String, String)])(
        implicit rds: Writes[A],
        hc: HeaderCarrier,
        ec: ExecutionContext): Future[HttpResponse] =
    super.doPut(url, body, headers)(rds, buildHeaderCarrier(hc), ec)

  override def doPatch[A](url: String, body: A, headers: Seq[(String, String)])(
        implicit rds: Writes[A],
        hc: HeaderCarrier,
        ec: ExecutionContext): Future[HttpResponse] =
    super.doPatch(url, body, headers)(rds, buildHeaderCarrier(hc), ec)
}
