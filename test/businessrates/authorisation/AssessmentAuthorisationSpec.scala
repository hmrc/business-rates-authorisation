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
import businessrates.authorisation.services.AccountsService
import businessrates.authorisation.utils.{StubAuthConnector, StubOrganisationAccounts, StubPersonAccounts, StubPropertyLinking}
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class AssessmentAuthorisationSpec extends ControllerSpec with MockitoSugar with BeforeAndAfterEach {

  override protected def afterEach(): Unit = {
    //reset to initial state
    when(mockAccountsService.get(anyString, anyString)(any[HeaderCarrier])).thenReturn(Future.successful(None))
  }

  lazy val mockAccountsService = {
    val m = mock[AccountsService]
    when(m.get(anyString, anyString)(any[HeaderCarrier])).thenReturn(Future.successful(None))
    m
  }

  private def stubAccounts(p: Person = person, o: Organisation = organisation) = {
    when(mockAccountsService.get(matching(p.externalId), matching(o.groupId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(Accounts(o.id, p.individualId, o, p))))
  }

  val testController = new AuthorisationController(StubAuthConnector, StubPropertyLinking, mockAccountsService)

  private val organisation: Organisation = randomOrganisation

  private val person: Person = randomPerson

  "Calling the assessment authorisation endpoint" when {
    "the user is not logged in to government gateway" must {
      "return a 401 response and the INVALID_GATEWAY_SESSION error code" in {
        val res = testController.authoriseToViewAssessment(123, 456)(FakeRequest())
        status(res) mustBe UNAUTHORIZED
        contentAsJson(res) mustBe Json.obj("errorCode" -> "INVALID_GATEWAY_SESSION")
      }
    }

    "the user is logged in to government gateway but has not registered a VOA account" must {
      "return a 401 response and the NO_CUSTOMER_RECORD error code" in {
        StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, organisation.groupId, "Organisation"))
        val res = testController.authoriseToViewAssessment(123, 456)(FakeRequest())
        status(res) mustBe UNAUTHORIZED
        contentAsJson(res) mustBe Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")
      }
    }

    "the user is logged in to government gateway and has a VOA account" when {
      "the account does not have a link to the property" must {
        "return a 403 response" in {
          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, organisation.groupId, "Organisation"))
          stubAccounts()
          val res = testController.authoriseToViewAssessment(123, 456)(FakeRequest())
          status(res) mustBe FORBIDDEN
        }
      }

      "the account has an approved property link, but is not linked to the specific assessment" must {
        "return a 403 response" in {
          val linkId = 1234
          val assessmentRef = 9012

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, organisation.groupId, "Organisation"))
          stubAccounts()
          StubPropertyLinking.stubLink(PropertyLink(linkId, 1111, organisation.id, person.individualId, LocalDate.now, false, Seq(Assessment(assessmentRef + 1, "2017", 1111, LocalDate.now)), Seq(), "APPROVED"))
          val res = testController.authoriseToViewAssessment(linkId, assessmentRef)(FakeRequest())
          status(res) mustBe FORBIDDEN
        }
      }

      "the account has a pending property link" must {
        "return a 403 response" in {
          val linkId = 2345
          val assessmentRef = 1234

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, organisation.groupId, "Organisation"))
          stubAccounts()
          StubPropertyLinking.stubLink(PropertyLink(linkId, 1111, organisation.id, person.individualId, LocalDate.now, true, Seq(Assessment(assessmentRef, "2017", 1111, LocalDate.now)), Seq(), "PENDING"))

          val res = testController.authoriseToViewAssessment(linkId, assessmentRef)(FakeRequest())
          status(res) mustBe FORBIDDEN
        }
      }
      
      "the account has a valid link to the assessment" must {
        "return a 200 response and the organisation and person IDs" in {
          val linkId = 1234
          val assessmentRef = 9012

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(person.externalId, organisation.groupId, "Organisation"))
          stubAccounts()
          StubPropertyLinking.stubLink(PropertyLink(linkId, 1111, organisation.id, person.individualId, LocalDate.now, false, Seq(Assessment(assessmentRef, "2017", 1111, LocalDate.now)), Seq(), "APPROVED"))
          val res = testController.authoriseToViewAssessment(linkId, assessmentRef)(FakeRequest())
          status(res) mustBe OK
          contentAsJson(res) mustBe Json.obj("organisationId" -> organisation.id, "personId" -> person.individualId, "organisation" -> Json.toJson(organisation), "person" -> Json.toJson(person))
        }
      }

      "the account is acting as an agent on behalf of the property link" must {
        "return a 200 response, the organisation and person IDs, and the organisation and person account details" in {
          val anAgent: Person = randomPerson
          val agentOrganisation: Organisation = randomOrganisation

          StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(anAgent.externalId, agentOrganisation.groupId, "Organisation"))
          stubAccounts(anAgent, agentOrganisation)

          val propertyLink: PropertyLink = randomPropertyLink.retryUntil(_.organisationId != agentOrganisation.id).copy(pending = false, agents = Seq(randomParty.copy(organisationId = agentOrganisation.id)))
          StubPropertyLinking.stubLink(propertyLink)

          val res = testController.authoriseToViewAssessment(propertyLink.authorisationId, propertyLink.assessment.head.assessmentRef)(FakeRequest())
          status(res) mustBe OK
          contentAsJson(res) mustBe Json.obj("organisationId" -> agentOrganisation.id, "personId" -> anAgent.individualId, "organisation" -> Json.toJson(agentOrganisation), "person" -> Json.toJson(anAgent))
        }
      }
    }
  }
}
