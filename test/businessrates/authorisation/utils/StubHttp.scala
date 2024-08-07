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

package businessrates.authorisation.utils

import businessrates.authorisation.connectors.VOABackendWSHttp
import businessrates.authorisation.utils.TestConfiguration._
import org.apache.pekko.actor.ActorSystem
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Writes
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import scala.concurrent.{ExecutionContext, Future}

object StubHttp
    extends VOABackendWSHttp(
      configuration,
      mock[Metrics],
      mock[AuditConnector],
      mock[WSClient],
      mock[ActorSystem],
      mock[HttpAuditing]) {

  override def doGet(url: String, headers: Seq[(String, String)])(implicit ec: ExecutionContext): Future[HttpResponse] =
    ???

  override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(
        implicit rds: Writes[A],
        ec: ExecutionContext): Future[HttpResponse] = ???

  override def doFormPost(url: String, body: Map[String, Seq[String]], headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] = ???

  override def doPostString(url: String, body: String, headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] = ???

  override def doEmptyPost[A](url: String, headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] =
    ???

  override val hooks: Seq[HttpHook] = Seq.empty
}
