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

package businessrates.authorisation.controllers

import businessrates.authorisation.models.{Accounts, GovernmentGatewayDetails}
import businessrates.authorisation.services.AccountsService
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait WithIds extends Results with AuthorisedFunctions {

  protected val accounts: AccountsService

  def withIds(default: Accounts => Future[Result])(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Result] =
    authorised()
      .retrieve(v2.Retrievals.externalId and v2.Retrievals.groupIdentifier and v2.Retrievals.affinityGroup) {
        case Some(externalId) ~ Some(groupId) ~ Some(affinityGroup) =>
          impl(default)(GovernmentGatewayDetails(externalId, Some(groupId), Some(affinityGroup)))
        case _ =>
          Future.successful(Unauthorized(Json.obj("errorCode" -> "INVALID_GATEWAY_SESSION")))
      }
      .recover {
        case _: AuthorisationException => Unauthorized(Json.obj("errorCode" -> "INVALID_GATEWAY_SESSION"))
        case e                         => throw e
      }

  protected def impl(default: Accounts => Future[Result])(
        optGGDetails: GovernmentGatewayDetails)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Result]

}

class VoaIds @Inject()(
      val authConnector: AuthConnector,
      val accounts: AccountsService
) extends WithIds with Logging {

  override def impl(default: Accounts => Future[Result])(
        optGGDetails: GovernmentGatewayDetails)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Result] =
    optGGDetails match {
      case GovernmentGatewayDetails(externalId, Some(groupId), Some(Organisation) | Some(Individual)) =>
        accounts.get(externalId, groupId) flatMap {
          case Some(accs) => default(accs)
          case None       => Future.successful(Unauthorized(Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")))
        }
      case GovernmentGatewayDetails(_, None, _) =>
        logger.info(s"User has logged in with no groupId")
        Future.successful(Unauthorized(Json.obj("errorCode" -> "NON_GROUPID_ACCOUNT")))
      case GovernmentGatewayDetails(_, _, affinityGroup) =>
        logger.info(s"User has logged in with non-permitted affinityGroup ${affinityGroup.getOrElse("Not provided")}")
        Future.successful(Unauthorized(Json.obj("errorCode" -> "INVALID_ACCOUNT_TYPE")))
    }
}
