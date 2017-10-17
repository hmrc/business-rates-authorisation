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
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.config.{RunMode, ServicesConfig}
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class SimpleWSHttp extends WSHttp {
  override val hooks: Seq[HttpHook] = NoneRequired
}

class VOABackendWSHttp @Inject()(val metrics: Metrics) extends WSHttp with HasMetrics with AzureHeaders {
  override val hooks: Seq[HttpHook] = NoneRequired
}

object MicroserviceAuditConnector extends AuditConnector with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")
}

object MicroserviceAuthConnector extends AuthConnector with ServicesConfig {
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
