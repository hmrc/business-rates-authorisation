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
import businessrates.authorisation.models.{any => anyPT, _}
import businessrates.authorisation.services.AccountsService
import businessrates.authorisation.utils.{StubAuthConnector, StubOrganisationAccounts, StubPersonAccounts, StubPropertyLinking}
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class PropertyLinkAuthorisationSpec extends ControllerSpec with MockitoSugar with BeforeAndAfterEach {

  override protected def afterEach(): Unit = {
    //reset to initial state
    when(mockAccountsService.get(anyString, anyString)(any[HeaderCarrier])).thenReturn(Future.successful(None))
  }

  lazy val mockAccountsService = {
    val m = mock[AccountsService]
    when(m.get(anyString, anyString)(any[HeaderCarrier])).thenReturn(Future.successful(None))
    m
  }

  private def stubAccounts(p: Person, o: Organisation) = {
    when(mockAccountsService.get(matching(p.externalId), matching(o.groupId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(Accounts(o.id, p.individualId, o, p))))
  }

  private object TestAuthController extends AuthorisationController(StubAuthConnector, StubPropertyLinking, mockAccountsService)

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
        StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(randomShortString, Some(randomShortString), Some("Organisation")))

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

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, Some(organisation.groupId), Some("Organisation")))
          stubAccounts(person, organisation)

          val res = TestAuthController.authorise(randomPositiveLong)(FakeRequest())
          status(res) mustBe FORBIDDEN
        }
      }

      "the account has a pending link to the property" must {
        "return a 200 response, the organisation and person IDs, and the organisation and person account details" in {
          val person: Person = randomPerson
          val organisation: Organisation = randomOrganisation

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, Some(organisation.groupId), Some("Organisation")))
          stubAccounts(person, organisation)

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

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, Some(organisation.groupId), Some("Organisation")))
          stubAccounts(person, organisation)

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

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(anAgent.externalId, Some(agentOrganisation.groupId), Some("Organisation"))
          stubAccounts(anAgent, agentOrganisation)

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
