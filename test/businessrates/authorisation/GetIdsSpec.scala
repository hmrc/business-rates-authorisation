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
import org.scalacheck.Arbitrary.arbitrary

class GetIdsSpec extends ControllerSpec {

  val testController = new AuthorisationController(StubAuthConnector, StubGroupAccounts, StubPropertyLinking, StubIndividualAccounts)

  "Getting the IDs" when {
    "a user is submitting a check on their own property link" must {
      "return the same IDs for the case creator's IDs and the link owner's IDs" in {
        StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(client.externalId, clientOrganisation.groupId, "Organisation"))
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
        StubAuthConnector.stubAuthentication(GovernmentGatewayDetails(agent.externalId, agentOrganisation.groupId, "Organisation"))
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

  private lazy val clientOrganisation: Organisation = randomOrganisation

  private lazy val client: Person = randomPerson

  private lazy val agentOrganisation: Organisation = randomOrganisation.copy(isAgent = true)

  private lazy val agent: Person = randomPerson

  private lazy val authorisationId: Int = arbitrary[Int]
  private lazy val assessmentRef: Int = arbitrary[Int]
}
