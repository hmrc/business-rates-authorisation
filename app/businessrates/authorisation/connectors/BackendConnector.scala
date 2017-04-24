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

import businessrates.authorisation.models._
import com.google.inject.name.Named
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

class BackendConnector @Inject()(@Named("voaBackendWSHttp") val http: WSHttp,
                                 @Named("dataPlatformUrl") val backendUrl: String,
                                 @Named("ratesListYear") val listYear: Int)
  extends GroupAccounts with IndividualAccounts with PropertyLinking {

  type AgentFilter = Party => Boolean
  type AuthFilter = PropertyLink => Boolean

  val groupAccountsUrl = s"$backendUrl/customer-management-api/organisation"
  val individualAccountsUrl: String = s"$backendUrl/customer-management-api/person"
  val agentRepresentationRequestUrl: String = s"$backendUrl/mdtp-dashboard-management-api/mdtp_dashboard/agent_representation_requests"
  val authorisationsUrl: String = s"$backendUrl/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment"

  val declinedStatuses = Seq("REVOKED", "DECLINED")

  private val onlyPendingAndApproved: AgentFilter = agent => List("APPROVED", "PENDING").contains(agent.authorisedPartyStatus)
  private val mustHaveAPermission: AgentFilter = _.permissions.nonEmpty
  private val withoutPermissionEndDate: Party => Party = agent => agent.copy(permissions = agent.permissions.filterNot(_.endDate.isDefined))

  private def NotFound[T]: PartialFunction[Throwable, Option[T]] = {
    case _: NotFoundException => None
  }

  private def withAuthId(id: Long): AuthFilter = p => p.authorisationId == id

  private def withAgentOrg(agentId: Long): AuthFilter = a => a.agents.exists(_.organisationId == agentId)

  private def withAuthIdAndAgent(authId: Long, agentId: Long): AuthFilter = a => withAuthId(authId)(a) && withAgentOrg(agentId)(a)

  def getOrganisationByGGId(ggId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]] =
    getOrganisation(ggId, "governmentGatewayGroupId")

  def getOrganisationByOrgId(orgId: Long)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]] =
    getOrganisation(s"$orgId", "organisationId")

  def getPerson(externalId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Person]] =
    http.GET[Option[Person]](s"$individualAccountsUrl?governmentGatewayExternalId=$externalId") recover NotFound[Person]

  def getLink(organisationId: Long, authorisationId: Long)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PropertyLink]] = {
    getProperty(authorisationId, organisationId)
  }

  override def getAssessment(organisationId: Long, authorisationId: Long, assessmentRef: Long)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Assessment]] = {

    getLink(organisationId, authorisationId) map {
      case Some(link) if !link.pending => link.assessment.find(_.assessmentRef == assessmentRef)
      case None => None
    }
  }

  private def getOrganisation(id: String, paramName: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]] =
    http.GET[Option[Organisation]](s"$groupAccountsUrl?$paramName=$id") recover NotFound[Organisation]

  private def getProperty(authorisationId: Long, organisationId: Long)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PropertyLink]] = {
    getAuthorisation(authorisationId).map(_.find(l => l.organisationId == organisationId || l.agents.exists(_.organisationId == organisationId)))
  }

  private def getAuthorisation(authorisationId: Long)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PropertyLink]] = {
    val url = s"$authorisationsUrl" +
      s"?listYear=$listYear" +
      s"&authorisationId=$authorisationId"

    http.GET[Option[PropertyLink]](url) map {
      case Some(link) if !declinedStatuses.contains(link.authorisationStatus.toUpperCase) =>
        Some(link.copy(agents = link.agents.filter(onlyPendingAndApproved).map(withoutPermissionEndDate).filter(mustHaveAPermission)))
      case None => None
    } recover NotFound[PropertyLink]
  }
}
