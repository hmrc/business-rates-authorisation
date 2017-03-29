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
import businessrates.authorisation.models.{AccountIds, Accounts, GovernmentGatewayIds, SubmissionIds}
import cats.data.OptionT
import cats.instances.future._
import play.api.libs.json.Json
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
    withIds { accountIds =>
      Future.successful(Ok(Json.toJson(accountIds)))
    }
  }

  def authorise(authorisationId: Long, assessmentRef: Long) = Action.async { implicit request =>
    withIds { case a@Accounts(oid, pid, _, _) =>
      val hasAssessmentRef = (for {
        propertyLinks <- OptionT(propertyLinking.find(oid, authorisationId))
        assessment <- OptionT.fromOption(propertyLinks.assessment.find(_.assessmentRef == assessmentRef))
      } yield assessment).value

      hasAssessmentRef.map {
        case Some(_) => Ok(Json.toJson(a))
        case None => Forbidden
      }.recover { case _ => Forbidden }
    }
  }

  def getIds(authorisationId: Long) = Action.async { implicit request =>
    withIds { case Accounts(oid, pid, _, _) =>
      propertyLinking.find(oid, authorisationId) map {
        case Some(link) => Ok(Json.toJson(SubmissionIds(caseCreator = AccountIds(oid, pid), interestedParty = AccountIds(link.organisationId, link.personId))))
        case None => Forbidden
      }
    }
  }

  private def withIds(default: Accounts => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    authConnector.getGovernmentGatewayIds flatMap {
      case Some(GovernmentGatewayIds(externalId, groupId)) =>
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
      case None => Future.successful(Unauthorized(Json.obj("errorCode" -> "INVALID_GATEWAY_SESSION")))
    }
  }
}
