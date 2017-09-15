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
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpReads, NotFoundException}
import HttpReads._
import org.joda.time.LocalDate
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

class BackendConnector @Inject()(@Named("voaBackendWSHttp") val http: WSHttp,
                                 @Named("dataPlatformUrl") val backendUrl: String,
                                 @Named("ratesListYear") val listYear: Int)
  extends OrganisationAccounts with PersonAccounts with PropertyLinking {

  type AgentFilter = Party => Boolean

  val groupAccountsUrl = s"$backendUrl/customer-management-api/organisation"
  val individualAccountsUrl: String = s"$backendUrl/customer-management-api/person"
  val authorisationsUrl: String = s"$backendUrl/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment"

  val declinedStatuses = Seq("REVOKED", "DECLINED")

  private val onlyPendingAndApproved: AgentFilter = agent => Seq(RepresentationStatus.approved, RepresentationStatus.pending)
    .contains(agent.authorisedPartyStatus)
  private val mustHaveAPermission: AgentFilter = _.permissions.exists(p => p.values.exists { case (_, a) => a != NotPermitted })
  private val withoutPermissionEndDateOrAfterNow: Party => Party =
    agent => agent.copy(permissions = agent.permissions.filter(p => p.endDate.forall(ed => ed.isAfter(LocalDate.now))))

  private def NotFound[T]: PartialFunction[Throwable, Option[T]] = {
    case _: NotFoundException => None
  }

  def getOrganisationByGGId(ggId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]] =
    getOrganisation(ggId, "governmentGatewayGroupId")

  def getOrganisationByOrgId(orgId: Long)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]] =
    getOrganisation(s"$orgId", "organisationId")

  def getPerson(externalId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Person]] = {
    implicit val apiFormat = Person.apiFormat
    http.GET[Option[Person]](s"$individualAccountsUrl?governmentGatewayExternalId=$externalId") recover NotFound[Person]
  }

  def getLink(organisationId: Long, authorisationId: Long)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PropertyLink]] = {
    getAuthorisation(authorisationId).map(_.find(l => l.organisationId == organisationId || l.agents.exists(_.organisationId == organisationId)))
  }

  private def withRole(role: PermissionType): Permission => Boolean = { p => role == any || p.values.get(role).exists(_ != NotPermitted) }
  
  override def getAssessment(organisationId: Long, authorisationId: Long, assessmentRef: Long, role: PermissionType)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Assessment]] = {
    getLink(organisationId, authorisationId) map {
      case PendingLink() => None
      case PropertyLinkOwnerAndAssessments(`organisationId`, assessments) => assessments.find(_.assessmentRef == assessmentRef)
      case PropertyLinkAssessmentsAndAgents(assessments, agents) =>
        agents.find(_.organisationId == organisationId).flatMap {
          case Party(permissions, RepresentationStatus.approved, _) if permissions exists withRole(role) =>
            assessments.find(_.assessmentRef == assessmentRef)
          case _ => None
        }
      case _ => None
    }
  }

  private def getOrganisation(id: String, paramName: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]] = {
    implicit val apiFormat = Organisation.apiFormat
    http.GET[Option[Organisation]](s"$groupAccountsUrl?$paramName=$id") recover NotFound[Organisation]
  }

  protected def getAuthorisation(authorisationId: Long)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PropertyLink]] = {
    http.GET[Option[PropertyLink]](s"$authorisationsUrl?listYear=$listYear&authorisationId=$authorisationId") map {
      case Some(link) if !declinedStatuses.contains(link.authorisationStatus.toUpperCase) =>
        Some(link.copy(agents = link.agents.filter(onlyPendingAndApproved).map(withoutPermissionEndDateOrAfterNow).filter(mustHaveAPermission)))
      case _ => None
    } recover NotFound[PropertyLink]
  }
}
