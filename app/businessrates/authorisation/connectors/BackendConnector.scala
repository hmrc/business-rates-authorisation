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

import businessrates.authorisation.models.{Organisation, Person, PropertyLink}
import com.google.inject.name.Named
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class BackendConnector @Inject()(@Named("voaBackendWSHttp") val http: WSHttp, val config: ServicesConfig)
  extends GroupAccounts with IndividualAccounts with PropertyLinking {

  private def NotFound[T]: PartialFunction[Throwable, Option[T]] = { case _: NotFoundException => None }

  lazy val backendUrl: String = config.baseUrl("data-platform")

  lazy val groupAccountsUrl = s"$backendUrl/customer-management-api/organisation"
  lazy val individualAccountsUrl: String = s"$backendUrl/customer-management-api/person"

  lazy val propertyLinkingUrl: String = s"${config.baseUrl("property-linking")}/property-linking"
  lazy val linkedPropertiesUrl: String = s"$propertyLinkingUrl/property-links"

  def getOrganisation(ggGroupId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]] = {
    http.GET[Option[Organisation]](s"$groupAccountsUrl?governmentGatewayGroupId=$ggGroupId") recover NotFound[Organisation]
  }

  def getPerson(externalId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Person]] = {
    http.GET[Option[Person]](s"$individualAccountsUrl?governmentGatewayExternalId=$externalId") recover NotFound[Person]
  }

  def getLink(organisationId: Long, authorisationId: Long)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PropertyLink]] = {
    http.GET[Seq[PropertyLink]](s"$linkedPropertiesUrl/$organisationId") map {
      _.find( link => link.authorisationId == authorisationId && !link.pending )
    }
  }
}
