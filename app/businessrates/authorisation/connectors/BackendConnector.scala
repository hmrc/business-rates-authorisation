/*
 * Copyright 2023 HM Revenue & Customs
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

import businessrates.authorisation.connectors.BackendConnector.UpdateCredentialsSuccess
import businessrates.authorisation.models.{Organisation, Person}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait BackendConnector {

  def getOrganisationByGGId(
        ggId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]]

  def getPerson(externalId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Person]]

  def updateCredentials(personId: String, groupId: String, externalId: String)(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[UpdateCredentialsSuccess.type]
}

object BackendConnector {
  case object UpdateCredentialsSuccess
}
