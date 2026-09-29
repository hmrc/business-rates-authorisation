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

package businessrates.authorisation.controllers

import businessrates.authorisation.controllers.AuthorisationController._
import businessrates.authorisation.models.Accounts
import businessrates.authorisation.services.AccountsService
import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthorisationController @Inject() (
      val accountsService: AccountsService,
      controllerComponents: ControllerComponents,
      val authConnector: AuthConnector
)(implicit executionContext: ExecutionContext)
    extends BackendController(controllerComponents) with AuthorisedFunctions with Logging {

  def authenticate: Action[AnyContent] =
    Action.async { implicit request =>
      getAccounts().map {
        case Authenticated(accounts) =>
          Ok(toJson(accounts))
        case err: AuthenticationErrorResult =>
          Unauthorized(Json.obj("errorCode" -> err.reason))
      }
    }

  def authenticationStatus: Action[AnyContent] =
    Action.async { implicit request =>
      getAccounts()
        .map {
          case Authenticated(accounts) =>
            Ok(Json.obj("authenticated" -> true, "accounts" -> accounts))
          case error: AuthenticationErrorResult =>
            Ok(Json.obj("authenticated" -> false, "errorCode" -> error.reason))
        }
    }

  private def getAccounts()(implicit hc: HeaderCarrier): Future[AuthenticationResult] =
    authorised()
      .retrieve(externalId and groupIdentifier and affinityGroup and allEnrolments) {
        case Some(externalId) ~ Some(groupId) ~ Some(Individual | Organisation) ~ enrolments =>
          accountsService.get(externalId, groupId, enrolments).map {
            case Some(accounts) =>
              Authenticated(accounts)
            case None =>
              NoCustomerRecord
          }
        case Some(_) ~ Some(_) ~ Some(otherAffinityGroup) ~ _ =>
          logger.info(s"User has logged in with non-permitted affinityGroup $otherAffinityGroup")
          Future.successful(InvalidAccountType)
        case _ =>
          Future.successful(InvalidGatewaySession)
      }
      .recover {
        case _: AuthorisationException => InvalidGatewaySession
        case e                         => throw e
      }

}

object AuthorisationController {
  private sealed trait AuthenticationResult
  private sealed trait AuthenticationErrorResult extends AuthenticationResult {
    val reason: String
  }
  private case class Authenticated(accounts: Accounts) extends AuthenticationResult
  private case object NoCustomerRecord extends AuthenticationErrorResult {
    override val reason: String = "NO_CUSTOMER_RECORD"
  }

  private case object InvalidAccountType extends AuthenticationErrorResult {
    override val reason: String = "INVALID_ACCOUNT_TYPE"
  }
  private case object InvalidGatewaySession extends AuthenticationErrorResult {
    override val reason: String = "INVALID_GATEWAY_SESSION"
  }
}
