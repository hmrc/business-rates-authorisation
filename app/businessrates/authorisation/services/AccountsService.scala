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

package businessrates.authorisation.services

import businessrates.authorisation.connectors.ModernisedBackendConnector
import businessrates.authorisation.models.Accounts
import businessrates.authorisation.repositories.AccountsCache
import cats.data.OptionT
import cats.implicits._
import play.api.Logging
import uk.gov.hmrc.auth.core.{EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AccountsService @Inject() (
      modernisedConnector: ModernisedBackendConnector,
      accountsCache: AccountsCache
)(implicit ec: ExecutionContext)
    extends Logging {

  def get(externalId: String, groupId: String, enrolments: Enrolments)(implicit
        hc: HeaderCarrier
  ): Future[Option[Accounts]] =
    for {
      optCachedAccounts: Option[Accounts] <- hc.sessionId match {
                                               case Some(sessionId) => accountsCache.get(sessionId.value)
                                               case None            => Future.successful(None)
                                             }
      optAccounts <- optCachedAccounts match {
                       case Some(cachedAccounts) =>
                         Future.successful(Some(cachedAccounts))
                       case None =>
                         getBackendData(externalId, groupId).flatMap {
                           case Some(accounts) =>
                             Future.successful(Some(accounts))
                           case None =>
                             enrolments.getEnrolment("HMRC-VOA-CCA").flatMap(_.getIdentifier("VOAPersonID")) match {
                               case Some(EnrolmentIdentifier(_, enrolmentPersonId)) =>
                                 for {
                                   _ <- modernisedConnector.updateCredentials(enrolmentPersonId, groupId, externalId)
                                   _ = logger.info(s"External ID and Group ID updated for personID: $enrolmentPersonId")
                                   updatedAccounts <- getBackendData(externalId, groupId)
                                 } yield updatedAccounts
                               case None =>
                                 Future.successful(Option.empty[Accounts])
                             }
                         }
                     }
      _ <- (hc.sessionId, optAccounts) match {
             case (Some(sessionId), Some(accounts)) if optCachedAccounts.isEmpty =>
               accountsCache.cache(sessionId.value, accounts)
             case _ =>
               Future.successful(())
           }
    } yield optAccounts

  def getBackendData(externalId: String, groupId: String)(implicit hc: HeaderCarrier): Future[Option[Accounts]] =
    (for {
      organisation <- OptionT(modernisedConnector.getOrganisationByGGId(groupId))
      person       <- OptionT(modernisedConnector.getPerson(externalId))
    } yield Accounts(
      organisation.id,
      person.individualId,
      organisation.copy(agentCode = organisation.agentCode.filter(_ => organisation.isAgent)),
      person
    )).value
}
