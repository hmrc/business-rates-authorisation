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

import businessrates.authorisation.connectors.ModernisedBackendConnector
import businessrates.authorisation.models.{Organisation, Person}
import businessrates.authorisation.utils.TestConfiguration.servicesConfig
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class StubBackendConnector extends ModernisedBackendConnector(StubHttp, servicesConfig) {
  private var stubbedOrganisation: Option[Organisation] = None
  private var stubbedPerson: Option[Person] = None

  def reset() = {
    stubbedPerson = None
    stubbedOrganisation = None
  }

  def stubOrganisation(organisation: Organisation) = stubbedOrganisation = Some(organisation)
  def stubPerson(person: Person) = stubbedPerson = Some(person)

  override def getOrganisationByGGId(
        ggGroupId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]] =
    Future.successful(stubbedOrganisation)

  override def getPerson(externalId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Person]] =
    Future.successful(stubbedPerson)

}
