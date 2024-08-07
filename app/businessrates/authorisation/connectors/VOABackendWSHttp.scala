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

package businessrates.authorisation.connectors

import org.apache.pekko.actor.ActorSystem
import businessrates.authorisation.metrics.HasMetrics
import com.typesafe.config.Config
import play.api.Configuration
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import uk.gov.hmrc.play.http.ws._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VOABackendWSHttp @Inject()(
      config: Configuration,
      override val metrics: Metrics,
      override val auditConnector: AuditConnector,
      override val wsClient: WSClient,
      override val actorSystem: ActorSystem,
      httpAuditing: HttpAuditing)
    extends DefaultHttpClient(
      config = config,
      httpAuditing = httpAuditing,
      wsClient = wsClient,
      actorSystem = actorSystem) with HasMetrics with HttpAuditing {

  override val hooks: Seq[HttpHook] = Seq(AuditingHook)

  override def appName: String = "business-rates-authorisation"

  override def doGet(url: String, headers: Seq[(String, String)])(
        implicit
        ec: ExecutionContext): Future[HttpResponse] =
    super.doGet(url, headers)(ec)

}
