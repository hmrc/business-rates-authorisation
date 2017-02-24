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
import businessrates.authorisation.models.{AccountIds, GovernmentGatewayIds, SubmissionIds}
import play.api.libs.json.Json
import play.api.mvc._
import cats.data.OptionT
import cats.instances.future._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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
    withIds { case a@AccountIds(oid, pid) =>
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
    // TODO: requires agent API integration
    withIds { case a@AccountIds(oid, pid) =>
      Future.successful(Ok(Json.toJson(SubmissionIds(a, AccountIds(oid, pid)))))
    }
  }

  private def withIds(default: AccountIds => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    authConnector.getGovernmentGatewayIds flatMap {
      case Some(GovernmentGatewayIds(externalId, groupId)) =>
        val getOrganisationId = groupAccounts.getOrganisationId(groupId)
        val getPersonId = individualAccounts.getPersonId(externalId)

        for {
          organisationId <- getOrganisationId
          personId <- getPersonId
          res <- (organisationId, personId) match {
            case (Some(oid), Some(pid)) => default(AccountIds(oid, pid))
            case _ => Future.successful(Unauthorized(Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")))
          }
        } yield {
          res
        }
      case None => Future.successful(Unauthorized(Json.obj("errorCode" -> "INVALID_GATEWAY_SESSION")))
    }
  }
}
