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

import businessrates.authorisation.config.WSHttp
import businessrates.authorisation.models._
import com.google.inject.name.Named
import javax.inject.Inject
import uk.gov.hmrc.http.HttpReads._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

class BackendConnector @Inject()(
      @Named("voaBackendWSHttp") val http: WSHttp,
      @Named("dataPlatformUrl") val backendUrl: String,
      @Named("ratesListYear") val listYear: Int)
    extends OrganisationAccounts with PersonAccounts with PropertyLinking {

  type AgentFilter = Party => Boolean

  val groupAccountsUrl = s"$backendUrl/customer-management-api/organisation"
  val individualAccountsUrl: String = s"$backendUrl/customer-management-api/person"
  val authorisationsUrl: String = s"$backendUrl/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment"

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
    implicit val apiFormat = Person.apiFormat
    http.GET[Option[Person]](s"$individualAccountsUrl?governmentGatewayExternalId=$externalId") recover NotFound[
      Person]
  }

  def getLink(organisationId: Long, authorisationId: Long)(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[Option[PropertyLink]] =
    getAuthorisation(authorisationId).map(_.find(l =>
      l.organisationId == organisationId || l.agents.exists(_.organisationId == organisationId)))

  override def getAssessment(organisationId: Long, authorisationId: Long, assessmentRef: Long)(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[Option[Assessment]] =
    getLink(organisationId, authorisationId) map {
      case PendingLink() => None
      case PropertyLinkOwnerAndAssessments(`organisationId`, assessments) =>
        assessments.find(_.assessmentRef == assessmentRef)
      case PropertyLinkAssessmentsAndAgents(assessments, agents) =>
        agents
          .find(_.organisationId == organisationId)
          .flatMap(_ => assessments.find(_.assessmentRef == assessmentRef))
      case _ => None
    }

  private def getOrganisation(id: String, paramName: String)(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[Option[Organisation]] = {
    implicit val apiFormat = Organisation.apiFormat
    http.GET[Option[Organisation]](s"$groupAccountsUrl?$paramName=$id") recover NotFound[Organisation]
  }

  protected def getAuthorisation(
        authorisationId: Long)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PropertyLink]] =
    http.GET[Option[PropertyLink]](s"$authorisationsUrl?listYear=$listYear&authorisationId=$authorisationId") map {
      case Some(link) if !declinedStatuses.contains(link.authorisationStatus.toUpperCase) =>
        Some(link)
      case _ => None
    } recover NotFound[PropertyLink]
}
