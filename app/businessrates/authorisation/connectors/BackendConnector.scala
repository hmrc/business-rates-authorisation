/*
 * Copyright 2022 HM Revenue & Customs
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

import businessrates.authorisation.models._
import javax.inject.Inject
import play.api.libs.json.Reads
import uk.gov.hmrc.http.HttpReads._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class BackendConnector @Inject()(val http: VOABackendWSHttp, servicesConfig: ServicesConfig)
    extends OrganisationAccounts with PersonAccounts {

  lazy val backendUrl: String = servicesConfig.baseUrl("data-platform")
  lazy val listYear: Int = servicesConfig.getConfInt("rates.list.year", 2017)

  type AgentFilter = Party => Boolean

  val groupAccountsUrl = s"$backendUrl/customer-management-api/organisation"
  val individualAccountsUrl: String = s"$backendUrl/customer-management-api/person"

  val declinedStatuses = Seq("REVOKED", "DECLINED")

  private def NotFound[T]: PartialFunction[Throwable, Option[T]] = {
    case _: NotFoundException => None
  }

  def getOrganisationByGGId(
        ggId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]] =
    getOrganisation(ggId, "governmentGatewayGroupId")

  def getOrganisationByOrgId(
        orgId: Long)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]] =
    getOrganisation(s"$orgId", "organisationId")

  def getPerson(externalId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Person]] = {
    implicit val apiFormat: Reads[Person] = Person.apiFormat
    http.GET[Option[Person]](s"$individualAccountsUrl?governmentGatewayExternalId=$externalId") recover NotFound[Person]
  }

  private def getOrganisation(id: String, paramName: String)(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[Option[Organisation]] = {
    implicit val apiFormat: Reads[Organisation] = Organisation.apiFormat
    http.GET[Option[Organisation]](s"$groupAccountsUrl?$paramName=$id") recover NotFound[Organisation]
  }

}
