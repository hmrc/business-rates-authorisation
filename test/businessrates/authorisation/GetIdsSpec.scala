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
import businessrates.authorisation.utils.StubPropertyLinking
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
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

class GetIdsSpec extends ControllerSpec with ArbitraryDataGeneration with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]

  override protected def afterEach(): Unit =
    //reset to initial state
    when(mockAccountsService.get(anyString, anyString)(any[HeaderCarrier])).thenReturn(Future.successful(None))

  lazy val mockAccountsService = {
    val m = mock[AccountsService]
    when(m.get(anyString, anyString)(any[HeaderCarrier])).thenReturn(Future.successful(None))
    m
  }

  private def stubAccounts(p: Person, o: Organisation) =
    when(mockAccountsService.get(matching(p.externalId), matching(o.groupId))(any[HeaderCarrier]))
      .thenReturn(Future.successful(Some(Accounts(o.id, p.individualId, o, p))))

  val testController = new AuthorisationController(
    StubPropertyLinking,
    mockAccountsService,
    new VoaIds(mockAuthConnector, mockAccountsService),
    stubControllerComponents())

  "Getting the IDs" when {
    "a user is submitting a check on their own property link" should {
      "return the same IDs for the case creator's IDs and the link owner's IDs" in {
        when(
          mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
          .thenReturn(Future.successful(
            new ~(new ~(Some(client.externalId), Some(clientOrganisation.groupId)), Some(AffinityGroup.Organisation))))
        stubAccounts(client, clientOrganisation)
        StubPropertyLinking.stubLink(
          PropertyLink(
            authorisationId,
            1111,
            clientOrganisation.id,
            client.individualId,
            LocalDate.now,
            pending = false,
            Seq(Assessment(assessmentRef + 1, "2017", 1111, LocalDate.now)),
            Seq(),
            "APPROVED"
          ))

        val res = testController.getIds(authorisationId)(FakeRequest())
        status(res) shouldBe OK

        contentAsJson(res) shouldBe Json.obj(
          "caseCreator"     -> Json.obj("organisationId" -> clientOrganisation.id, "personId" -> client.individualId),
          "interestedParty" -> Json.obj("organisationId" -> clientOrganisation.id, "personId" -> client.individualId)
        )
      }
    }

    "an agent is submitting a check on behalf of a client" should {
      "return the agent's IDs as the case creator's IDs, and the client's IDs as the link owner's IDs" in {
        when(
          mockAuthConnector.authorise(
            any(),
            matching(Retrievals.externalId and Retrievals.groupIdentifier and Retrievals.affinityGroup))(any(), any()))
          .thenReturn(Future.successful(
            new ~(new ~(Some(agent.externalId), Some(agentOrganisation.groupId)), Some(AffinityGroup.Organisation))))
        stubAccounts(agent, agentOrganisation)

        val party = randomParty.copy(organisationId = agentOrganisation.id)
        StubPropertyLinking.stubLink(
          PropertyLink(
            authorisationId,
            1111,
            clientOrganisation.id,
            client.individualId,
            LocalDate.now,
            false,
            Seq(Assessment(assessmentRef + 1, "2017", 1111, LocalDate.now)),
            Seq(party),
            "APPROVED"
          )
        )

        val res = testController.getIds(authorisationId)(FakeRequest())
        status(res) shouldBe OK

        contentAsJson(res) shouldBe Json.obj(
          "caseCreator"     -> Json.obj("organisationId" -> agentOrganisation.id, "personId"  -> agent.individualId),
          "interestedParty" -> Json.obj("organisationId" -> clientOrganisation.id, "personId" -> client.individualId)
        )
      }
    }
  }

  private lazy val clientOrganisation: Organisation = randomOrganisation

  private lazy val client: Person = randomPerson

  private lazy val agentOrganisation: Organisation = randomOrganisation.copy(isAgent = true)

  private lazy val agent: Person = randomPerson

  private lazy val authorisationId: Int = arbitrary[Int]
  private lazy val assessmentRef: Int = arbitrary[Int]
}
