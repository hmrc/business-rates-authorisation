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
import businessrates.authorisation.models.GovernmentGatewayIds
import play.api.libs.json.Json
import play.api.mvc._
import cats.data.OptionT
import cats.instances.future._
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AuthorisationController @Inject()(val authConnector: AuthConnector,
                                        val groupAccounts: GroupAccounts,
                                        val propertyLinking: PropertyLinking,
                                        val individualAccounts: IndividualAccounts
                                       ) extends BaseController {

  def authenticate = Action.async { implicit request =>
    authConnector.getGovernmentGatewayIds flatMap {
      case Some(GovernmentGatewayIds(externalId, groupId)) => for {
        organisationId <- groupAccounts.getOrganisationId(groupId)
        personId <- individualAccounts.getPersonId(externalId)
      } yield {
        (organisationId, personId) match {
          case (Some(oid), Some(pid)) => Ok(Json.obj("organisationId" -> oid, "personId" -> pid))
          case _ => Unauthorized(Json.obj("errorCode" -> "NO_CUSTOMER_RECORD"))
        }
      }
      case None => Future.successful(Unauthorized(Json.obj("errorCode" -> "INVALID_GATEWAY_SESSION")))
    }
  }

	def forAssessment(linkId: String, assessmentRef: Long) = Action.async { implicit request =>
    val hasAssessmentRef = (for {
      ids <- OptionT(authConnector.getGovernmentGatewayIds)
      organisationId <- OptionT(groupAccounts.getOrganisationId(ids.groupId))
      pLinks <- OptionT.liftF(propertyLinking.linkedProperties(organisationId))
    } yield
      pLinks.filter(_.linkId == linkId).flatMap(_.assessment.map(_.asstRef))
      ).value
      .map(_.toList.flatten)
      .map(_.contains(assessmentRef))

    hasAssessmentRef.map {
      case true => Ok
      case false => Forbidden
    }.recover{ case _ => Forbidden}
  }

}
