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
import businessrates.authorisation.models._
import businessrates.authorisation.utils.{StubAuthConnector, StubGroupAccounts, StubIndividualAccounts, StubPropertyLinking}
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

class AuthorisationSpec extends ControllerSpec {

  val testController = new AuthorisationController(StubAuthConnector, StubGroupAccounts, StubPropertyLinking, StubIndividualAccounts)

  private val organisation: Organisation = randomOrganisation

  private val person: Person = randomPerson

  "Calling the check authorisation endpoint" when {
    "the user is not logged in to government gateway" must {
      "return a 401 response and the INVALID_GATEWAY_SESSION error code" in {
        val res = testController.authorise(123, 456)(FakeRequest())
        status(res) mustBe UNAUTHORIZED
        contentAsJson(res) mustBe Json.obj("errorCode" -> "INVALID_GATEWAY_SESSION")
      }
    }

    "the user is logged in to government gateway but has not registered a VOA account" must {
      "return a 401 response and the NO_CUSTOMER_RECORD error code" in {
        StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, organisation.groupId, "Organisation"))
        val res = testController.authorise(123, 456)(FakeRequest())
        status(res) mustBe UNAUTHORIZED
        contentAsJson(res) mustBe Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")
      }
    }

    "the user is logged in to government gateway and has a VOA account" when {
      "the account does not have a link to the property" must {
        "return a 403 response" in {
          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, organisation.groupId, "Organisation"))
          StubGroupAccounts.stubOrganisation(organisation)
          StubIndividualAccounts.stubPerson(person)
          val res = testController.authorise(123, 456)(FakeRequest())
          status(res) mustBe FORBIDDEN
        }
      }

      "the account has an approved property link, but is not linked to the specific assessment" must {
        "return a 403 response" in {
          val linkId = 1234
          val assessmentRef = 9012

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, organisation.groupId, "Organisation"))
          StubGroupAccounts.stubOrganisation(organisation)
          StubIndividualAccounts.stubPerson(person)
          StubPropertyLinking.stubLink(PropertyLink(linkId, 1111, organisation.id, person.individualId, DateTime.now, false, Seq(Assessment(assessmentRef + 1, "2017", 1111, LocalDate.now))))
          val res = testController.authorise(linkId, assessmentRef)(FakeRequest())
          status(res) mustBe FORBIDDEN
        }
      }

      "the account has a pending property link" must {
        "return a 403 response" in {
          val linkId = 2345
          val assessmentRef = 1234

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, organisation.groupId, "Organisation"))
          StubGroupAccounts.stubOrganisation(organisation)
          StubIndividualAccounts.stubPerson(person)
          StubPropertyLinking.stubLink(PropertyLink(linkId, 1111, organisation.id, person.individualId, DateTime.now, true, Seq(Assessment(assessmentRef, "2017", 1111, LocalDate.now))))

          val res = testController.authorise(linkId, assessmentRef)(FakeRequest())
          status(res) mustBe FORBIDDEN
        }
      }
      
      "the account has a valid link to the assessment" must {
        "return a 200 response and the organisation and person IDs" in {
          val linkId = 1234
          val assessmentRef = 9012

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, organisation.groupId, "Organisation"))
          StubGroupAccounts.stubOrganisation(organisation)
          StubIndividualAccounts.stubPerson(person)
          StubPropertyLinking.stubLink(PropertyLink(linkId, 1111, organisation.id, person.individualId, DateTime.now, false, Seq(Assessment(assessmentRef, "2017", 1111, LocalDate.now))))
          val res = testController.authorise(linkId, assessmentRef)(FakeRequest())
          status(res) mustBe OK
          contentAsJson(res) mustBe Json.obj("organisationId" -> organisation.id, "personId" -> person.individualId, "organisation" -> Json.toJson(organisation), "person" -> Json.toJson(person))
        }
      }
    }
  }
}
