/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDate

import businessrates.authorisation.controllers.{AuthorisationController, VoaIds}
import businessrates.authorisation.models._
import businessrates.authorisation.services.AccountsService
import businessrates.authorisation.utils._
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AssessmentAuthorisationSpec extends ControllerSpec with MockitoSugar with BeforeAndAfterEach {

  val maybeOrg = Some("Organisation")

  val mockAuthConnector = mock[AuthConnector]

  override protected def beforeEach(): Unit = {
    StubPropertyLinking.reset()
    Mockito.reset(mockAuthConnector)
    StubOrganisationAccounts.reset()
    StubPersonAccounts.reset()
  }

  override protected def afterEach(): Unit =
    //reset to initial state
    when(mockAccountsService.get(any(), any())(any())).thenReturn(Future.successful(None))

  lazy val mockAccountsService = {
    val m = mock[AccountsService]
    when(m.get(anyString, anyString)(any())).thenReturn(Future.successful(None))
    m
  }

  private def stubAccounts(p: Person = person, o: Organisation = organisation) =
    when(mockAccountsService.get(matching(p.externalId), matching(o.groupId))(any[HeaderCarrier]))
      .thenReturn(Future.successful(Some(Accounts(o.id, p.individualId, o, p))))

  val testController = new AuthorisationController(
    StubPropertyLinking,
    mockAccountsService,
    new VoaIds(mockAuthConnector, mockAccountsService),
    stubControllerComponents())

  private val organisation: Organisation = randomOrganisation

  private val person: Person = randomPerson

  "Calling the assessment authorisation endpoint" when {

    "the user is logged in to government gateway but has not registered a VOA account" should {
      "return a 401 response and the NO_CUSTOMER_RECORD error code" in {
        when(
          mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
          .thenReturn(Future.successful(new ~(new ~(Some(""), Some("")), Some(AffinityGroup.Organisation))))

        val res = testController.authoriseToViewAssessment(123, 456)(FakeRequest())
        status(res) shouldBe UNAUTHORIZED
        contentAsJson(res) shouldBe Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")
      }
    }

    "the user is logged in to government gateway and has a VOA account" when {
      "the account does not have a link to the property" should {
        "return a 403 response" in {
          when(mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
            .thenReturn(Future.successful(
              new ~(new ~(Some(person.externalId), Some(organisation.groupId)), Some(AffinityGroup.Organisation))))
          stubAccounts()
          val res = testController.authoriseToViewAssessment(123, 456)(FakeRequest())
          status(res) shouldBe FORBIDDEN
        }
      }

      "the account has an approved property link, but is not linked to the specific assessment" should {
        "return a 403 response" in {
          val linkId = 1234
          val assessmentRef = 9012

          when(mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
            .thenReturn(Future.successful(
              new ~(new ~(Some(person.externalId), Some(organisation.groupId)), Some(AffinityGroup.Organisation))))
          stubAccounts()
          StubPropertyLinking.stubLink(
            PropertyLink(
              linkId,
              1111,
              organisation.id,
              person.individualId,
              LocalDate.now,
              false,
              Seq(Assessment(assessmentRef + 1, "2017", 1111, LocalDate.now)),
              Seq(),
              "APPROVED"))
          val res = testController.authoriseToViewAssessment(linkId, assessmentRef)(FakeRequest())
          status(res) shouldBe FORBIDDEN
        }
      }

      "the account has a pending property link" should {
        "return a 403 response" in {
          val linkId = 2345
          val assessmentRef = 1234

          when(mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
            .thenReturn(Future.successful(
              new ~(new ~(Some(person.externalId), Some(organisation.groupId)), Some(AffinityGroup.Organisation))))
          stubAccounts()
          StubPropertyLinking.stubLink(
            PropertyLink(
              linkId,
              1111,
              organisation.id,
              person.individualId,
              LocalDate.now,
              true,
              Seq(Assessment(assessmentRef, "2017", 1111, LocalDate.now)),
              Seq(),
              "PENDING"))

          val res = testController.authoriseToViewAssessment(linkId, assessmentRef)(FakeRequest())
          status(res) shouldBe FORBIDDEN
        }
      }

      "the account has a valid link to the assessment" should {
        "return a 200 response and the organisation and person IDs" in {
          val linkId = 1234
          val assessmentRef = 9012

          when(mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
            .thenReturn(Future.successful(
              new ~(new ~(Some(person.externalId), Some(organisation.groupId)), Some(AffinityGroup.Organisation))))
          stubAccounts()
          StubPropertyLinking.stubLink(
            PropertyLink(
              linkId,
              1111,
              organisation.id,
              person.individualId,
              LocalDate.now,
              false,
              Seq(Assessment(assessmentRef, "2017", 1111, LocalDate.now)),
              Seq(),
              "APPROVED"))
          val res = testController.authoriseToViewAssessment(linkId, assessmentRef)(FakeRequest())
          status(res) shouldBe OK
          contentAsJson(res) shouldBe Json.obj(
            "organisationId" -> organisation.id,
            "personId"       -> person.individualId,
            "organisation"   -> Json.toJson(organisation),
            "person"         -> Json.toJson(person))
        }
      }

      "the account is acting as an agent on behalf of the property link" should {
        "return a 200 response, the organisation and person IDs, and the organisation and person account details" in {
          val anAgent: Person = randomPerson
          val agentOrganisation: Organisation = randomOrganisation

          when(
            mockAuthConnector.authorise(
              any(),
              matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(
              any(),
              any())).thenReturn(Future.successful(
            new ~(new ~(Some(anAgent.externalId), Some(agentOrganisation.groupId)), Some(AffinityGroup.Organisation))))
          stubAccounts(anAgent, agentOrganisation)

          val propertyLink: PropertyLink = randomPropertyLink
            .retryUntil(_.organisationId != agentOrganisation.id)
            .copy(
              pending = false,
              agents = Seq(
                randomParty.copy(
                  organisationId = agentOrganisation.id
                ))
            )
          StubPropertyLinking.stubLink(propertyLink)

          val res = testController.authoriseToViewAssessment(
            propertyLink.authorisationId,
            propertyLink.assessment.head.assessmentRef)(FakeRequest())
          status(res) shouldBe OK
          contentAsJson(res) shouldBe Json.obj(
            "organisationId" -> agentOrganisation.id,
            "personId"       -> anAgent.individualId,
            "organisation"   -> Json.toJson(agentOrganisation),
            "person"         -> Json.toJson(anAgent))
        }
      }
    }

    "the user logs in and has an invalid government gateway OR VOA account " when {
      "auth service" should {
        "return a 401 without group id" in {
          val anAgent: Person = randomPerson
          val agentOrganisation: Organisation = randomOrganisation

          when(mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
            .thenReturn(Future.successful(
              new ~(new ~(Some(person.externalId), Some(organisation.groupId)), Some(AffinityGroup.Organisation))))

          stubAccounts(anAgent, agentOrganisation)

          val propertyLink: PropertyLink = randomPropertyLink
            .retryUntil(_.organisationId != agentOrganisation.id)
            .copy(
              pending = false,
              agents = Seq(
                randomParty.copy(
                  organisationId = agentOrganisation.id
                ))
            )
          StubPropertyLinking.stubLink(propertyLink)

          val res = testController.authoriseToViewAssessment(
            propertyLink.authorisationId,
            propertyLink.assessment.head.assessmentRef)(FakeRequest())
          status(res) shouldBe UNAUTHORIZED
        }
        "return a 401 without affinity group" in {
          val anAgent: Person = randomPerson
          val agentOrganisation: Organisation = randomOrganisation

          when(mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
            .thenReturn(Future.successful(
              new ~(new ~(Some(person.externalId), Some(organisation.groupId)), Some(AffinityGroup.Organisation))))

          stubAccounts(anAgent, agentOrganisation)

          val propertyLink: PropertyLink = randomPropertyLink
            .retryUntil(_.organisationId != agentOrganisation.id)
            .copy(
              pending = false,
              agents = Seq(
                randomParty.copy(
                  organisationId = agentOrganisation.id
                ))
            )
          StubPropertyLinking.stubLink(propertyLink)

          val res = testController.authoriseToViewAssessment(
            propertyLink.authorisationId,
            propertyLink.assessment.head.assessmentRef)(FakeRequest())
          status(res) shouldBe UNAUTHORIZED
        }

      }
    }
  }
}
