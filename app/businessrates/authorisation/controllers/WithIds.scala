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

package businessrates.authorisation.controllers

import javax.inject.Inject

import businessrates.authorisation.connectors.AuthConnector
import businessrates.authorisation.models.{Accounts, GovernmentGatewayDetails}
import businessrates.authorisation.services.AccountsService
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait WithIds extends Results {
  protected val authConnector: AuthConnector
  protected val accounts: AccountsService

  def withIds(default: Accounts => Future[Result])(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Result] =
    authConnector.getGovernmentGatewayDetails.flatMap(impl(default))

  protected def impl(default: (Accounts) => Future[Result])
                    (optGGDetails: Option[GovernmentGatewayDetails])
                    (implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Result]

}

class EnrolmentIds @Inject()(
                              val authConnector: AuthConnector,
                              val accounts: AccountsService
                            ) extends WithIds {

  override def impl(default: (Accounts) => Future[Result])
                   (optGGDetails: Option[GovernmentGatewayDetails])
                   (implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Result] = {
    optGGDetails match {
      case Some(GovernmentGatewayDetails(externalId, Some(groupId), Some("Organisation") | Some("Individual"))) =>
        accounts.get(externalId, groupId) flatMap {
          case Some(accs) => default(accs)
          case None => Future.successful(Unauthorized(Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")))
        }
      case Some(GovernmentGatewayDetails(_, None, _)) =>
        Logger.info(s"User has logged in with no groupId")
        Future.successful(Unauthorized(Json.obj("errorCode" -> "NON_GROUPID_ACCOUNT")))
      case Some(GovernmentGatewayDetails(_, _, affinityGroup)) =>
        Logger.info(s"User has logged in with non-permitted affinityGroup ${affinityGroup.getOrElse("Not provided")}")
        Future.successful(Unauthorized(Json.obj("errorCode" -> "NON_ORGANISATION_ACCOUNT")))
      case None => Future.successful(Unauthorized(Json.obj("errorCode" -> "INVALID_GATEWAY_SESSION")))
    }
  }
}

class NonEnrolment @Inject()(
                              val authConnector: AuthConnector,
                              val accounts: AccountsService
                            ) extends WithIds {

  override def impl(default: (Accounts) => Future[Result])
                   (optGGDetails: Option[GovernmentGatewayDetails])
                   (implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Result] = {
    optGGDetails match {
      case Some(GovernmentGatewayDetails(externalId, Some(groupId), Some("Organisation"))) =>
        accounts.get(externalId, groupId) flatMap {
          case Some(accs) => default(accs)
          case None => Future.successful(Unauthorized(Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")))
        }
      case Some(GovernmentGatewayDetails(_, None, _)) =>
        Logger.info(s"User has logged in with no groupId")
        Future.successful(Unauthorized(Json.obj("errorCode" -> "NON_GROUPID_ACCOUNT")))
      case Some(GovernmentGatewayDetails(_, _, affinityGroup)) =>
        Logger.info(s"User has logged in with non-permitted affinityGroup ${affinityGroup.getOrElse("Not provided")}")
        Future.successful(Unauthorized(Json.obj("errorCode" -> "NON_ORGANISATION_ACCOUNT")))
      case None => Future.successful(Unauthorized(Json.obj("errorCode" -> "INVALID_GATEWAY_SESSION")))
    }
  }
}
