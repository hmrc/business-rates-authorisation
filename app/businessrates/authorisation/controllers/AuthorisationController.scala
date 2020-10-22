/*
 * Copyright 2020 HM Revenue & Customs
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

import businessrates.authorisation.connectors._
import businessrates.authorisation.models._
import businessrates.authorisation.services.AccountsService
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthorisationController @Inject()(
      val propertyLinking: PropertyLinking,
      val accounts: AccountsService,
      val ids: WithIds,
      controllerComponents: ControllerComponents
) extends BackendController(controllerComponents) {

  import ids._

  def authenticate: Action[AnyContent] = Action.async { implicit request =>
    withIds { accounts =>
      Future successful Ok(toJson(accounts))
    }
  }

  def authoriseToViewAssessment(authorisationId: Long, assessmentRef: Long): Action[AnyContent] = Action.async {
    implicit request =>
      withIds { accounts =>
        propertyLinking.getAssessment(accounts.organisationId, authorisationId, assessmentRef).map {
          case Some(_) => Ok(toJson(accounts))
          case _       => Forbidden
        }
      }
  }

  def authorise(authorisationId: Long): Action[AnyContent] = Action.async { implicit request =>
    withIds {
      case a @ Accounts(oid, _, _, _) =>
        propertyLinking.getLink(oid, authorisationId) map {
          case Some(_) => Ok(Json.toJson(a))
          case None    => Forbidden
        }
    }
  }

  def getIds(authorisationId: Long): Action[AnyContent] = Action.async { implicit request =>
    withIds {
      case Accounts(oid, pid, _, _) =>
        propertyLinking.getLink(oid, authorisationId).map {
          case Some(link) =>
            Ok(
              toJson(
                SubmissionIds(
                  caseCreator = AccountIds(oid, pid),
                  interestedParty = AccountIds(link.organisationId, link.personId)
                )))
          case None => Forbidden
        }
    }
  }
}
