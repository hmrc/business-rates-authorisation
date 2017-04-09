/*
 * Copyright 2017 HM Revenue & Customs
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

import businessrates.authorisation.connectors._
import businessrates.authorisation.models.{AccountIds, Accounts, GovernmentGatewayDetails, SubmissionIds}
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthorisationController @Inject()(val authConnector: AuthConnector,
                                        val groupAccounts: GroupAccounts,
                                        val propertyLinking: PropertyLinking,
                                        val individualAccounts: IndividualAccounts
                                       ) extends BaseController {

  def authenticate = Action.async { implicit request =>
    withIds { accounts =>
      Future successful Ok(toJson(accounts))
    }
  }

  def authorise(authorisationId: Long, assessmentRef: Long) = Action.async { implicit request =>
    withIds { accounts =>
      propertyLinking.getLink(accounts.organisationId, authorisationId).map {
        case Some(link) if link.assessment.exists(_.assessmentRef == assessmentRef) => Ok(toJson(accounts))
        case _ => Forbidden
      }.recover { case _ => Forbidden }
    }
  }

  def getIds(authorisationId: Long) = Action.async { implicit request =>
    withIds { case Accounts(oid, pid, _, _) =>
      propertyLinking.getLink(oid, authorisationId).map {
        case Some(link) => Ok(toJson(SubmissionIds(caseCreator = AccountIds(oid, pid),
                                                   interestedParty = AccountIds(link.organisationId, link.personId))))
        case None => Forbidden
      }.recover { case _ => Forbidden }
    }
  }

  private def withIds(default: Accounts => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    authConnector.getGovernmentGatewayDetails flatMap {
      case Some(GovernmentGatewayDetails(externalId, groupId, "Organisation")) =>

        val eventualOrganisation = groupAccounts.getOrganisation(groupId)
        val eventualPerson = individualAccounts.getPerson(externalId)

        for {
          organisationId <- eventualOrganisation
          personId <- eventualPerson
          res <- (organisationId, personId) match {
            case (Some(o), Some(p)) => default(Accounts(o.id, p.individualId, o, p))
            case _ => Future.successful(Unauthorized(Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")))
          }
        } yield {
          res
        }

      case Some(GovernmentGatewayDetails(_, _, affinityGroup)) =>
        Logger.info(s"User has logged in with non-permitted affinityGroup $affinityGroup")
        Future.successful(Unauthorized(Json.obj("errorCode" -> "NON_ORGANISATION_ACCOUNT")))

      case None => Future.successful(Unauthorized(Json.obj("errorCode" -> "INVALID_GATEWAY_SESSION")))
    }
  }
}
