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

    "the user is logged in to Government Gateway but has not registered a CCA account" must {
      "return a 401 status and the NO_CUSTOMER_RECORD error code" in {
        StubAuthConnector.stubAuthentication(GovernmentGatewayIds("anExternalId", "aGroupId"))
        val res = testController.authenticate()(FakeRequest())
        status(res) mustBe UNAUTHORIZED
        contentAsJson(res) mustBe Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")
      }
    }

    "the user is logged in to Government Gateway and has registered a CCA account" must {
      "return a 200 status and the organisation ID, the person ID, and the organisation and person accounts" in {
        val stubOrganisation = Organisation(
          12345,
          "anotherGroupId",
          "some company",
          Address(Some(1), "The place", "", "", "", "AA11 1AA"),
          "email@address.com",
          "12345",
          false,
          false,
          1L
        )

        val stubPerson = Person(
          "anotherExternalId",
          "trustId",
          12345,
          67890,
          PersonDetails(
            "Not A",
            "Real Person",
            "aa@bb.cc",
            "123456",
            None,
            Address(Some(2), "The Road", "The Place", "", "", "AA11 1AA")
          )
        )

        StubAuthConnector.stubAuthentication(GovernmentGatewayIds(stubPerson.externalId, stubOrganisation.groupId))
        StubGroupAccounts.stubOrganisation(stubOrganisation)
        StubIndividualAccounts.stubPerson(stubPerson)
        val res = testController.authenticate()(FakeRequest())
        status(res) mustBe OK
        contentAsJson(res) mustBe Json.obj("organisationId" -> 12345, "personId" -> 67890, "organisation" -> Json.toJson(stubOrganisation), "person" -> Json.toJson(stubPerson))
      }
    }
  }
}
