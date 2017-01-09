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

import businessrates.authorisation.connectors.{AuthConnector, GroupAccounts, PropertyLinking}
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.play.microservice.controller.BaseController
import cats.data.OptionT
import cats.instances.future._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AuthorisationController @Inject()(val authConnector: AuthConnector,
                                        val groupAccounts: GroupAccounts,
                                        val propertyLinking: PropertyLinking
                                       ) extends BaseController {

	def forAssessment(linkId: String, assessmentRef: Long) = Action.async { implicit request =>
    val hasAssessmentRef = (for {
      ggGroupId <- OptionT.liftF(authConnector.getGGGroupId())
      organisationId <- OptionT(groupAccounts.getOrganisationId(ggGroupId))
      pLinks <- OptionT.liftF(propertyLinking.linkedProperties(organisationId))
    } yield
      pLinks.filter(_.linkId == linkId).flatMap(_.assessment)
      ).value
      .map(_.toList.flatten)
      .map(_.contains(assessmentRef))

    hasAssessmentRef.map(x => x match  {
      case true => Ok
      case false => Forbidden
    } ).recover{ case _ => Forbidden}
  }

}
