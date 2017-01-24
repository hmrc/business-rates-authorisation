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

package businessrates.authorisation.connectors

import javax.inject.Inject

import businessrates.authorisation.config.VOABackendWSHttp
import play.api.libs.json._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

class GroupAccounts @Inject()(val http: VOABackendWSHttp)(implicit ec: ExecutionContext) extends ServicesConfig {

  type OrganisationId = Int

  lazy val url = baseUrl("data-platform")

  def getOrganisationId(ggGroupId: String)(implicit hc: HeaderCarrier): Future[Option[OrganisationId]] = {
    http.GET[JsValue](s"$url/organisation?governmentGatewayExternalId=$ggGroupId") map { js => (js \ "id").asOpt[OrganisationId] } recover {
      case _: NotFoundException => None
    }
  }
}
