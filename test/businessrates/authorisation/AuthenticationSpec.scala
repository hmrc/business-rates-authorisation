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
import businessrates.authorisation.utils.{StubAuthConnector, StubGroupAccounts, StubIndividualAccounts, StubPropertyLinking}
import businessrates.authorisation.models._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

class AuthenticationSpec extends ControllerSpec {

  val testController = new AuthorisationController(StubAuthConnector, StubGroupAccounts, StubPropertyLinking, StubIndividualAccounts)

  "Calling the authentication endpoint" when {
    "the user is not logged in to Government Gateway" must {
      "return a 401 status and the INVALID_GATEWAY_SESSION error code" in {
        val res = testController.authenticate()(FakeRequest())
        status(res) mustBe UNAUTHORIZED
        contentAsJson(res) mustBe Json.obj("errorCode" -> "INVALID_GATEWAY_SESSION")
      }
    }

    "the user is logged in to Government Gateway with an individual account" must {
      "return a 401 status and the NON_ORGANISATION_ACCOUNT error code" in {
        StubAuthConnector.stubAuthentication(GovernmentGatewayDetails("anExternalId", "aGroupId", "Individual"))
        val res = testController.authenticate()(FakeRequest())
        status(res) mustBe UNAUTHORIZED
        contentAsJson(res) mustBe Json.obj("errorCode" -> "NON_ORGANISATION_ACCOUNT")
      }
    }

    "the user is logged in to Government Gateway with an agent account" must {
      "return a 401 status and the NON_ORGANISATION_ACCOUNT error code" in {
        StubAuthConnector.stubAuthentication(GovernmentGatewayDetails("anExternalId", "aGroupId", "Agent"))
        val res = testController.authenticate()(FakeRequest())
        status(res) mustBe UNAUTHORIZED
        contentAsJson(res) mustBe Json.obj("errorCode" -> "NON_ORGANISATION_ACCOUNT")
      }
    }

    "the user is logged in to Government Gateway with an organisation account but has not registered a CCA account" must {
      "return a 401 status and the NO_CUSTOMER_RECORD error code" in {
        StubAuthConnector.stubAuthentication(GovernmentGatewayDetails("anExternalId", "aGroupId", "Organisation"))
        val res = testController.authenticate()(FakeRequest())
        status(res) mustBe UNAUTHORIZED
        contentAsJson(res) mustBe Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")
      }
    }

    "the user is logged in to Government Gateway and has registered a CCA account" must {
      "return a 200 status and the organisation ID, the person ID, and the organisation and person accounts" in {
        val stubOrganisation: Organisation = randomOrganisation

        val stubPerson: Person = randomPerson

        StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(stubPerson.externalId, stubOrganisation.groupId, "Organisation"))
        StubGroupAccounts.stubOrganisation(stubOrganisation)
        StubIndividualAccounts.stubPerson(stubPerson)
        val res = testController.authenticate()(FakeRequest())
        status(res) mustBe OK
        contentAsJson(res) mustBe Json.obj(
          "organisationId" -> stubOrganisation.id,
          "personId" -> stubPerson.individualId,
          "organisation" -> Json.toJson(stubOrganisation),
          "person" -> Json.toJson(stubPerson)
        )
      }
    }
  }
}
