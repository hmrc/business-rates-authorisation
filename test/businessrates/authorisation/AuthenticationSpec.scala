/*
 * Copyright 2018 HM Revenue & Customs
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
import businessrates.authorisation.models.{Accounts, GovernmentGatewayDetails, Organisation, Person}
import businessrates.authorisation.services.AccountsService
import businessrates.authorisation.utils._
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AuthenticationSpec extends ControllerSpec with MockitoSugar {

  lazy val mockAccountsService = {
    val m = mock[AccountsService]
    when(m.get(anyString, anyString)(any[HeaderCarrier])).thenReturn(Future.successful(None))
    m
  }

  val enrolmentController = new AuthorisationController(StubAuthConnector, StubPropertyLinking, mockAccountsService, new VoaStubWithIds(mockAccountsService))

  "enrolmentController" should {
    behave like anAuthenticateEndpoint(enrolmentController, true)
  }

  def anAuthenticateEndpoint(testController: AuthorisationController, isEnrolmentController: Boolean = false) = {

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
          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails("anExternalId", Some("aGroupId"), Some("Individual")))
          val res = testController.authenticate()(FakeRequest())
          status(res) mustBe UNAUTHORIZED

          if (isEnrolmentController) {
            contentAsJson(res) mustBe Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")
          }
          else {
            contentAsJson(res) mustBe Json.obj("errorCode" -> "NON_ORGANISATION_ACCOUNT")
          }
        }
      }

      "the user is logged in to Government Gateway with an agent account" must {
        "return a 401 status and the NON_ORGANISATION_ACCOUNT error code" in {
          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails("anExternalId", Some("aGroupId"), Some("Agent")))
          val res = testController.authenticate()(FakeRequest())
          status(res) mustBe UNAUTHORIZED
          contentAsJson(res) mustBe Json.obj("errorCode" -> "NON_ORGANISATION_ACCOUNT")
        }
      }

      "the user is logged in to Government Gateway with an organisation account but has not registered a CCA account" must {
        "return a 401 status and the NO_CUSTOMER_RECORD error code" in {
          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails("anExternalId", Some("aGroupId"), Some("Organisation")))
          val res = testController.authenticate()(FakeRequest())
          status(res) mustBe UNAUTHORIZED
          contentAsJson(res) mustBe Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")
        }
      }

      "the user is logged in to Government Gateway and has registered a CCA account" must {
        "return a 200 status and the organisation ID, the person ID, and the organisation and person accounts" in {
          val stubOrganisation: Organisation = randomOrganisation

          val stubPerson: Person = randomPerson

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(stubPerson.externalId, Some(stubOrganisation.groupId), Some("Organisation")))
          when(mockAccountsService.get(matching(stubPerson.externalId), matching(stubOrganisation.groupId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(Accounts(stubOrganisation.id, stubPerson.individualId, stubOrganisation, stubPerson))))

          StubOrganisationAccounts.stubOrganisation(stubOrganisation)
          StubPersonAccounts.stubPerson(stubPerson)
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

      "return a 401 status and fail when group id is missing" in {
        val stubOrganisation: Organisation = randomOrganisation

        val stubPerson: Person = randomPerson

        StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(stubPerson.externalId, None, Some("Organisation")))
        when(mockAccountsService.get(matching(stubPerson.externalId), matching(stubOrganisation.groupId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(Accounts(stubOrganisation.id, stubPerson.individualId, stubOrganisation, stubPerson))))

        StubOrganisationAccounts.stubOrganisation(stubOrganisation)
        StubPersonAccounts.stubPerson(stubPerson)
        val res = testController.authenticate()(FakeRequest())
        status(res) mustBe UNAUTHORIZED
        println(contentAsJson(res))
      }

      "return a 401 status and fail when group id is missing and is not Organisation" in {
        val stubOrganisation: Organisation = randomOrganisation

        val stubPerson: Person = randomPerson

        StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(stubPerson.externalId, None, Some("Agent")))
        when(mockAccountsService.get(matching(stubPerson.externalId), matching(stubOrganisation.groupId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(Accounts(stubOrganisation.id, stubPerson.individualId, stubOrganisation, stubPerson))))

        StubOrganisationAccounts.stubOrganisation(stubOrganisation)
        StubPersonAccounts.stubPerson(stubPerson)
        val res = testController.authenticate()(FakeRequest())
        status(res) mustBe UNAUTHORIZED
        println(contentAsJson(res))
      }

      "return a 401 status when the affinity group is missing from the user." in {
        val stubOrganisation: Organisation = randomOrganisation

        val stubPerson: Person = randomPerson

        StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(stubPerson.externalId, Some(stubOrganisation.groupId), None))
        when(mockAccountsService.get(matching(stubPerson.externalId), matching(stubOrganisation.groupId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(Accounts(stubOrganisation.id, stubPerson.individualId, stubOrganisation, stubPerson))))

        StubOrganisationAccounts.stubOrganisation(stubOrganisation)
        StubPersonAccounts.stubPerson(stubPerson)
        val res = testController.authenticate()(FakeRequest())
        status(res) mustBe UNAUTHORIZED
      }
    }
  }

}
