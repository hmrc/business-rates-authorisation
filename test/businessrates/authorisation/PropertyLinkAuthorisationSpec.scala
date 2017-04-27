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

package businessrates.authorisation

import businessrates.authorisation.controllers.AuthorisationController
import businessrates.authorisation.models.{GovernmentGatewayDetails, Organisation, Person, PropertyLink}
import businessrates.authorisation.utils.{StubAuthConnector, StubGroupAccounts, StubIndividualAccounts, StubPropertyLinking}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

class PropertyLinkAuthorisationSpec extends ControllerSpec {

  private object TestAuthController extends AuthorisationController(StubAuthConnector, StubGroupAccounts, StubPropertyLinking, StubIndividualAccounts)

  "Calling the property link authorisation endpoint" when {
    "the user is not logged in to government gateway" must {
      "return a 401 response and the INVALID_GATEWAY_SESSION error code" in {
        val res = TestAuthController.authorise(randomPositiveLong)(FakeRequest())
        status(res) mustBe UNAUTHORIZED
        contentAsJson(res) mustBe Json.obj("errorCode" -> "INVALID_GATEWAY_SESSION")
      }
    }

    "the user is logged in to government gateway but has not registered a VOA account" must {
      "return a 401 response and the NO_CUSTOMER_RECORD error code" in {
        StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(randomShortString, randomShortString, "Organisation"))

        val res = TestAuthController.authorise(randomPositiveLong)(FakeRequest())
        status(res) mustBe UNAUTHORIZED
        contentAsJson(res) mustBe Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")
      }
    }

    "the user is logged in to government gateway and has a VOA account" when {
      "the account does not have a link to the property" must {
        "return a 403 response" in {
          val person: Person = randomPerson
          val organisation: Organisation = randomOrganisation

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, organisation.groupId, "Organisation"))
          StubGroupAccounts.stubOrganisation(organisation)
          StubIndividualAccounts.stubPerson(person)

          val res = TestAuthController.authorise(randomPositiveLong)(FakeRequest())
          status(res) mustBe FORBIDDEN
        }
      }

      "the account has a pending link to the property" must {
        "return a 200 response, the organisation and person IDs, and the organisation and person account details" in {
          val person: Person = randomPerson
          val organisation: Organisation = randomOrganisation

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, organisation.groupId, "Organisation"))
          StubGroupAccounts.stubOrganisation(organisation)
          StubIndividualAccounts.stubPerson(person)

          val propertyLink: PropertyLink = randomPropertyLink.copy(organisationId = organisation.id, pending = true)
          StubPropertyLinking.stubLink(propertyLink)

          val res = TestAuthController.authorise(propertyLink.authorisationId)(FakeRequest())
          status(res) mustBe OK
          contentAsJson(res) mustBe Json.obj("organisationId" -> organisation.id, "personId" -> person.individualId, "organisation" -> Json.toJson(organisation), "person" -> Json.toJson(person))
        }
      }

      "the account has an approved link to the property" must {
        "return a 200 response, the organisation and person IDs, and the organisation and person account details" in {
          val person: Person = randomPerson
          val organisation: Organisation = randomOrganisation

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, organisation.groupId, "Organisation"))
          StubGroupAccounts.stubOrganisation(organisation)
          StubIndividualAccounts.stubPerson(person)

          val propertyLink: PropertyLink = randomPropertyLink.copy(organisationId = organisation.id, pending = false)
          StubPropertyLinking.stubLink(propertyLink)

          val res = TestAuthController.authorise(propertyLink.authorisationId)(FakeRequest())
          status(res) mustBe OK
          contentAsJson(res) mustBe Json.obj("organisationId" -> organisation.id, "personId" -> person.individualId, "organisation" -> Json.toJson(organisation), "person" -> Json.toJson(person))
        }
      }

      "the user is acting as an agent on behalf of the property" must {
        "return a 200 response, the organisation and person IDs, and the organisation and person account details" in {
          val anAgent: Person = randomPerson
          val agentOrganisation: Organisation = randomOrganisation

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(anAgent.externalId, agentOrganisation.groupId, "Organisation"))
          StubGroupAccounts.stubOrganisation(agentOrganisation)
          StubIndividualAccounts.stubPerson(anAgent)

          val propertyLink: PropertyLink = randomPropertyLink.retryUntil(_.organisationId != agentOrganisation.id).copy(pending = false, agents = Seq(randomParty.copy(organisationId = agentOrganisation.id)))
          StubPropertyLinking.stubLink(propertyLink)

          val res = TestAuthController.authorise(propertyLink.authorisationId)(FakeRequest())
          status(res) mustBe OK
          contentAsJson(res) mustBe Json.obj("organisationId" -> agentOrganisation.id, "personId" -> anAgent.individualId, "organisation" -> Json.toJson(agentOrganisation), "person" -> Json.toJson(anAgent))
        }
      }
    }
  }
}
