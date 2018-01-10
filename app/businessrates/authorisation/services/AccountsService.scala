/*
 * Copyright 2018 HM Revenue & Customs
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

package businessrates.authorisation.services

import javax.inject.Inject

import businessrates.authorisation.connectors.{OrganisationAccounts, PersonAccounts}
import businessrates.authorisation.models.Accounts
import businessrates.authorisation.repositories.AccountsCache
import cats.data.OptionT
import cats.implicits._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

class AccountsService @Inject()(groupAccounts: OrganisationAccounts, individualAccounts: PersonAccounts, cache: AccountsCache)(implicit ec: ExecutionContext) {

  def get(externalId: String, groupId: String)(implicit hc: HeaderCarrier): Future[Option[Accounts]] = {
    hc.sessionId.fold(getFromApi(externalId, groupId)) { sid =>
      cache.get(sid.value) flatMap {
        case Some(accounts) => Future.successful(Some(accounts))
        case _ => getFromApiAndCache(sid.value, externalId, groupId)
      }
    }
  }

  private def getFromApi(externalId: String, groupId: String)(implicit hc: HeaderCarrier): Future[Option[Accounts]] = {
    val eventualPerson = OptionT(individualAccounts.getPerson(externalId))

    (for {
      organisation <- OptionT(groupAccounts.getOrganisationByGGId(groupId))
      person <- eventualPerson
    } yield {
      Accounts(organisation.id, person.individualId, organisation, person)
    }).value
  }

  private def getFromApiAndCache(sessionId: String, externalId: String, groupId: String)(implicit hc: HeaderCarrier): Future[Option[Accounts]] = {
    getFromApi(externalId, groupId) flatMap {
      case Some(accs) => cache.cache(sessionId, accs) map { _ => Some(accs) }
      case None => Future.successful(None)
    }
  }
}
