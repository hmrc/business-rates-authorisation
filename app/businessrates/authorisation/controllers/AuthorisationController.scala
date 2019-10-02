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

package businessrates.authorisation.controllers

import businessrates.authorisation.action.AuthenticatedActionBuilder
import javax.inject.Inject
import businessrates.authorisation.connectors._
import businessrates.authorisation.models._
import businessrates.authorisation.services.AccountsService
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc._
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthorisationController @Inject()(authenticated: AuthenticatedActionBuilder,
                                         val authConnector: AuthConnector,
                                        val propertyLinking: PropertyLinking,
                                        val accounts: AccountsService,
                                        val ids: WithIds
                                       ) extends BaseController {

  import ids._

  val logger = Logger(this.getClass.getName)

  def authenticate = authenticated.async { implicit request =>
    logger.debug(s"    content-type: ${request.contentType}")
    logger.debug(s"    headers: ${request.headers}")
    logger.debug(s"    body: ${request.body}")
    logger.debug(s"    session is empty: ${request.session.isEmpty}")
    logger.debug(s"    session: ${request.session}")
    logger.debug(s"    session auth: ${request.session.get("authToken")}")
    withIds { accounts =>
      Future successful Ok(toJson(accounts))
    }
  }


  def authoriseToViewAssessment(authorisationId: Long, assessmentRef: Long, role: Option[PermissionType] = None) = authenticated.async { implicit request =>
    withIds { accounts =>
      propertyLinking.getAssessment(accounts.organisationId, authorisationId, assessmentRef, role.getOrElse(any)).map {
        case Some(_) => Ok(toJson(accounts))
        case _ => Forbidden
      }
    }
  }

  def authorise(authorisationId: Long) = authenticated.async { implicit request =>
    withIds { case a@Accounts(oid, _, _, _) =>
      propertyLinking.getLink(oid, authorisationId) map {
        case Some(_) => Ok(Json.toJson(a))
        case None => Forbidden
      }
    }
  }

  def getIds(authorisationId: Long) = authenticated.async { implicit request =>
    withIds { case Accounts(oid, pid, _, _) =>
      propertyLinking.getLink(oid, authorisationId).map {
        case Some(link) => Ok(toJson(
          SubmissionIds(
            caseCreator = AccountIds(oid, pid),
            interestedParty = AccountIds(link.organisationId, link.personId)
          )))
        case None => Forbidden
      }
    }
  }
}
