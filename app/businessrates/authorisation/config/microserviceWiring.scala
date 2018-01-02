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

package businessrates.authorisation.config

import javax.inject.Inject

import businessrates.authorisation.metrics.HasMetrics
import com.kenshoo.play.metrics.Metrics
import play.api.libs.json.Writes
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.http.logging.LoggingDetails
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.microservice.config.LoadAuditingConfig

import scala.concurrent.Future
import scala.util.{Failure, Try}

trait WSHttp extends HttpGet with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpPatch with WSPatch with HttpDelete with WSDelete with Hooks with AppName
object WSHttp extends WSHttp

class SimpleWSHttp extends WSHttp {
  override def logResult[A](ld: LoggingDetails, method: String, uri: String, startAge: Long)(result: Try[A]) = result match {
    case Failure(ex: Upstream4xxResponse) if ex.upstreamResponseCode == 401 => {}
    case _ => super.logResult[A](ld, method, uri, startAge)(result)
  }
}

class VOABackendWSHttp @Inject()(val metrics: Metrics) extends WSHttp with HasMetrics with AzureHeaders

object MicroserviceAuditConnector extends AuditConnector {
  lazy val auditingConfig: AuditingConfig = LoadAuditingConfig(s"auditing")
}

trait Hooks extends HttpHooks with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  override lazy val auditConnector: AuditConnector = MicroserviceAuditConnector
}

object MicroserviceAuthConnector extends AuthConnector with ServicesConfig with WSHttp {
  override val authBaseUrl: String = baseUrl("auth")
}

trait AzureHeaders extends WSHttp {
  def buildHeaderCarrier(hc: HeaderCarrier): HeaderCarrier = HeaderCarrier(requestId = hc.requestId, sessionId = hc.sessionId)
    .withExtraHeaders(hc.extraHeaders: _*)

  override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    super.doGet(url)(buildHeaderCarrier(hc))

  override def doDelete(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = super.doDelete(url)(buildHeaderCarrier(hc))

  override def doPatch[A](url: String, body: A)(implicit w: Writes[A], hc: HeaderCarrier): Future[HttpResponse] =
    super.doPatch(url, body)(w, buildHeaderCarrier(hc))

  override def doPut[A](url: String, body: A)(implicit w: Writes[A], hc: HeaderCarrier): Future[HttpResponse] =
    super.doPut(url, body)(w, buildHeaderCarrier(hc))

  override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit w: Writes[A], hc: HeaderCarrier): Future[HttpResponse] =
    super.doPost(url, body, headers)(w, buildHeaderCarrier(hc))
}
