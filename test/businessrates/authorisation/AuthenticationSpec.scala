/*
 * Copyright 2023 HM Revenue & Customs
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

import businessrates.authorisation.controllers.{AuthorisationController, VoaIds}
import businessrates.authorisation.models.{Accounts, Organisation, Person}
import businessrates.authorisation.services.AccountsService
import businessrates.authorisation.utils._
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AuthenticationSpec extends ControllerSpec with MockitoSugar {

  lazy val mockAccountsService = {
    val m = mock[AccountsService]
    when(m.get(anyString, anyString)(any[HeaderCarrier])).thenReturn(Future.successful(None))
    m
  }

  val mockAuthConnector = mock[AuthConnector]

  val testController = new AuthorisationController(
    mockAccountsService,
    new VoaIds(mockAuthConnector, mockAccountsService),
    stubControllerComponents())(scala.concurrent.ExecutionContext.global)

  "testController" should {
    behave like anAuthenticateEndpoint(testController)
  }

  def anAuthenticateEndpoint(testController: AuthorisationController): Unit =
    "Calling the authentication endpoint" when {
      "the user is logged in to Government Gateway with an agent account" should {
        "return a 401 status and the INVALID_ACCOUNT_TYPE error code" in {
          when(mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
            .thenReturn(
              Future.successful(new ~(new ~(Some("anExternalId"), Some("aGroupId")), Some(AffinityGroup.Agent))))
          val res = testController.authenticate()(FakeRequest())
          status(res) shouldBe UNAUTHORIZED
          contentAsJson(res) shouldBe Json.obj("errorCode" -> "INVALID_ACCOUNT_TYPE")
        }
      }

      "the user is logged in to Government Gateway with an organisation account but has not registered a CCA account" should {
        "return a 401 status and the NO_CUSTOMER_RECORD error code" in {
          when(mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
            .thenReturn(
              Future.successful(new ~(new ~(Some("anExternalId"), Some("aGroupId")), Some(AffinityGroup.Organisation))))
          val res = testController.authenticate()(FakeRequest())
          status(res) shouldBe UNAUTHORIZED
          contentAsJson(res) shouldBe Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")
        }
      }

      "the user is logged in to Government Gateway and has registered a CCA account" should {
        "return a 200 status and the organisation ID, the person ID, and the organisation and person accounts" in {
          val stubOrganisation: Organisation = randomOrganisation

          val stubPerson: Person = randomPerson

          when(mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
            .thenReturn(
              Future.successful(
                new ~(
                  new ~(Some(stubPerson.externalId), Some(stubOrganisation.groupId)),
                  Some(AffinityGroup.Organisation))))

          when(
            mockAccountsService.get(matching(stubPerson.externalId), matching(stubOrganisation.groupId))(
              any[HeaderCarrier])).thenReturn(Future.successful(
            Some(Accounts(stubOrganisation.id, stubPerson.individualId, stubOrganisation, stubPerson))))

          StubOrganisationAccounts.stubOrganisation(stubOrganisation)
          StubPersonAccounts.stubPerson(stubPerson)
          val res = testController.authenticate()(FakeRequest())
          status(res) shouldBe OK
          contentAsJson(res) shouldBe Json.obj(
            "organisationId" -> stubOrganisation.id,
            "personId"       -> stubPerson.individualId,
            "organisation"   -> Json.toJson(stubOrganisation),
            "person"         -> Json.toJson(stubPerson)
          )
        }
      }

      "return a 401 status and fail when group id is missing" in {
        val stubOrganisation: Organisation = randomOrganisation

        val stubPerson: Person = randomPerson

        when(
          mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
          .thenReturn(
            Future.successful(new ~(new ~(Some(stubPerson.externalId), None), Some(AffinityGroup.Organisation))))

        when(
          mockAccountsService.get(matching(stubPerson.externalId), matching(stubOrganisation.groupId))(
            any[HeaderCarrier])).thenReturn(
          Future.successful(Some(Accounts(stubOrganisation.id, stubPerson.individualId, stubOrganisation, stubPerson))))

        StubOrganisationAccounts.stubOrganisation(stubOrganisation)
        StubPersonAccounts.stubPerson(stubPerson)
        val res = testController.authenticate()(FakeRequest())
        status(res) shouldBe UNAUTHORIZED
      }

      "return a 401 status and fail when group id is missing and is not Organisation" in {
        val stubOrganisation: Organisation = randomOrganisation

        val stubPerson: Person = randomPerson

        when(
          mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
          .thenReturn(Future.successful(new ~(new ~(Some(stubPerson.externalId), None), Some(AffinityGroup.Agent))))

        when(
          mockAccountsService.get(matching(stubPerson.externalId), matching(stubOrganisation.groupId))(
            any[HeaderCarrier])).thenReturn(
          Future.successful(Some(Accounts(stubOrganisation.id, stubPerson.individualId, stubOrganisation, stubPerson))))

        StubOrganisationAccounts.stubOrganisation(stubOrganisation)
        StubPersonAccounts.stubPerson(stubPerson)
        val res = testController.authenticate()(FakeRequest())
        status(res) shouldBe UNAUTHORIZED
      }

      "return a 401 status when the affinity group is missing from the user." in {
        val stubOrganisation: Organisation = randomOrganisation

        val stubPerson: Person = randomPerson

        when(
          mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
          .thenReturn(
            Future.successful(new ~(new ~(Some(stubPerson.externalId), Some(stubOrganisation.groupId)), None)))
        when(
          mockAccountsService.get(matching(stubPerson.externalId), matching(stubOrganisation.groupId))(
            any[HeaderCarrier])).thenReturn(
          Future.successful(Some(Accounts(stubOrganisation.id, stubPerson.individualId, stubOrganisation, stubPerson))))

        StubOrganisationAccounts.stubOrganisation(stubOrganisation)
        StubPersonAccounts.stubPerson(stubPerson)
        val res = testController.authenticate()(FakeRequest())
        status(res) shouldBe UNAUTHORIZED
      }
    }

}
