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

class GetIdsSpec extends ControllerSpec {

  val testController = new AuthorisationController(StubAuthConnector, StubGroupAccounts, StubPropertyLinking, StubIndividualAccounts)

  "Getting the IDs" when {
    "a user is submitting a check on their own property link" must {
      "return the same IDs for the case creator's IDs and the link owner's IDs" in {
        StubAuthConnector.stubAuthentication(GovernmentGatewayIds(client.externalId, clientOrganisation.groupId))
        StubGroupAccounts.stubOrganisation(clientOrganisation)
        StubIndividualAccounts.stubPerson(client)
        StubPropertyLinking.stubLink(PropertyLink(authorisationId, 1111, clientOrganisation.id, client.individualId, DateTime.now, false, Seq(Assessment(assessmentRef + 1, "2017", 1111, LocalDate.now))))

        val res = testController.getIds(authorisationId)(FakeRequest())
        status(res) mustBe OK

        contentAsJson(res) mustBe Json.obj(
          "caseCreator" -> Json.obj("organisationId" -> clientOrganisation.id, "personId" -> client.individualId),
          "interestedParty" -> Json.obj("organisationId" -> clientOrganisation.id, "personId" -> client.individualId)
        )
      }
    }

    "an agent is submitting a check on behalf of a client" must {
      "return the agent's IDs as the case creator's IDs, and the client's IDs as the link owner's IDs" in {
        StubAuthConnector.stubAuthentication(GovernmentGatewayIds(agent.externalId, agentOrganisation.groupId))
        StubGroupAccounts.stubOrganisation(agentOrganisation)
        StubIndividualAccounts.stubPerson(agent)
        StubPropertyLinking.stubAgentLink(
          agentOrganisation.id,
          PropertyLink(authorisationId, 1111, clientOrganisation.id, client.individualId, DateTime.now, false, Seq(Assessment(assessmentRef + 1, "2017", 1111, LocalDate.now)))
        )
        
        val res = testController.getIds(authorisationId)(FakeRequest())
        status(res) mustBe OK

        contentAsJson(res) mustBe Json.obj(
          "caseCreator" -> Json.obj("organisationId" -> agentOrganisation.id, "personId" -> agent.individualId),
          "interestedParty" -> Json.obj("organisationId" -> clientOrganisation.id, "personId" -> client.individualId)
        )
      }
    }
  }

  private lazy val clientOrganisation = Organisation(
    12345,
    "anotherGroupId",
    "some company",
    1,
    "email@address.com",
    "12345",
    false,
    false,
    1L
  )

  private lazy val client = Person(
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
      2
    )
  )

  private lazy val agentOrganisation = Organisation(
    54321,
    "agentGroupId",
    "some agent",
    1,
    "agent@address.com",
    "12345",
    false,
    true,
    2L
  )

  private lazy val agent = Person(
    "yetAnotherExternalId",
    "anotherTrustId",
    54321,
    9876,
    PersonDetails(
      "Definitely A",
      "Real Person",
      "aa@bb.cc",
      "123456",
      None,
      2
    )
  )

  private lazy val authorisationId = 1234
  private lazy val assessmentRef = 9012
}
