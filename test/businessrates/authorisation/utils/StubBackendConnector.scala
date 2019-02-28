/*
 * Copyright 2019 HM Revenue & Customs
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

import businessrates.authorisation.connectors.BackendConnector
import businessrates.authorisation.models.{Organisation, Person, PropertyLink}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

class StubBackendConnector extends BackendConnector(StubHttp, "http://locahost:9536", 2017) {
  private var stubbedOrganisation: Option[Organisation] = None
  private var stubbedPerson: Option[Person] = None
  private var stubbedLinks: Seq[PropertyLink] = Nil

  def reset() = {
    stubbedPerson = None
    stubbedOrganisation = None
    stubbedLinks = Nil
  }

  def stubOrganisation(organisation: Organisation) = stubbedOrganisation = Some(organisation)
  def stubPerson(person: Person) = stubbedPerson = Some(person)
  def stubLink(link: PropertyLink) = stubbedLinks :+= link

  override def getOrganisationByGGId(ggGroupId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]] =
    Future.successful(stubbedOrganisation)

  override def getOrganisationByOrgId(orgId: Long)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]] =
    Future.successful(stubbedOrganisation)

  override def getPerson(externalId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Person]] =
    Future.successful(stubbedPerson)

  override protected def getAuthorisation(authorisationId: Long)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PropertyLink]] = Future.successful {
    stubbedLinks.find(_.authorisationId == authorisationId)
  }
}
