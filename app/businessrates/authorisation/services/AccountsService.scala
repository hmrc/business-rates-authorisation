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

package businessrates.authorisation.services

import businessrates.authorisation.config.FeatureSwitch
import businessrates.authorisation.connectors.{BackendConnector, BstBackendConnector, ModernisedBackendConnector}
import businessrates.authorisation.models.Accounts
import businessrates.authorisation.repositories.AccountsCache
import cats.data.OptionT
import cats.implicits._
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AccountsService @Inject()(
      modernisedConnector: ModernisedBackendConnector,
      bstConnector: BstBackendConnector,
      accountsCache: AccountsCache,
      featureSwitch: FeatureSwitch)(implicit ec: ExecutionContext)
    extends Logging {

  lazy val connector: BackendConnector =
    if (featureSwitch.isBstDownstreamEnabled) bstConnector else modernisedConnector

  def get(externalId: String, groupId: String)(implicit hc: HeaderCarrier): Future[Option[Accounts]] =
    cachedAccounts {
      (for {
        organisation <- OptionT(connector.getOrganisationByGGId(groupId))
        person       <- OptionT(connector.getPerson(externalId))
      } yield {
        Accounts(
          organisation.id,
          person.individualId,
          organisation.copy(agentCode = organisation.agentCode.filter(_ => organisation.isAgent)),
          person)
      }).value
    }

  private def cachedAccounts(block: => Future[Option[Accounts]])(implicit hc: HeaderCarrier): Future[Option[Accounts]] =
    hc.sessionId.fold(block) { sessionId =>
      accountsCache.get(sessionId.value).flatMap {
        case None =>
          block.flatTap {
            case Some(accs) => accountsCache.cache(sessionId.value, accs)
            case None       => Future.successful(())
          }
        case accs => Future.successful(accs)
      }
    }
}
