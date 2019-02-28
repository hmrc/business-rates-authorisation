/*
 * Copyright 2019 HM Revenue & Customs
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

package businessrates.authorisation.services

import java.util.UUID

import businessrates.authorisation.ArbitraryDataGeneration
import businessrates.authorisation.connectors.{OrganisationAccounts, PersonAccounts}
import businessrates.authorisation.models.{Accounts, Organisation, Person}
import businessrates.authorisation.repositories.AccountsCache
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{eq => matching, _}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

class AccountsServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ArbitraryDataGeneration {

  "Accounts service" when {
    "the request does not have a session id" should {
      val organisation: Organisation = randomOrganisation
      val person: Person = randomPerson

      "get the account data from the API and not cache it" in {
        val (groupId, externalId): (String, String) = (randomShortString, randomShortString)
        val expected = Accounts(organisation.id, person.individualId, organisation, person)

        when(mockOrganisations.getOrganisationByGGId(anyString())(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(organisation)))
        when(mockPersons.getPerson(anyString())(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(person)))

        val res = accountsService.get(externalId, groupId)(HeaderCarrier())
        await(res) shouldBe Some(expected)

        verify(mockOrganisations, once).getOrganisationByGGId(matching(groupId))(any[HeaderCarrier], any[ExecutionContext])
        verify(mockPersons, once).getPerson(matching(externalId))(any[HeaderCarrier], any[ExecutionContext])
        verify(mockCache, never).get(anyString())
        verify(mockCache, never).cache(anyString, matching(expected))
      }
    }

    "the request does have a session id" when {
      val sid = UUID.randomUUID().toString
      val hc = HeaderCarrier(sessionId = Some(SessionId(sid)))

      "the user does not have an account" should {
        "not cache the result" in {
          val (groupId, externalId): (String, String) = (randomShortString, randomShortString)
          when(mockOrganisations.getOrganisationByGGId(anyString())(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(None))
          when(mockPersons.getPerson(anyString())(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(None))

          val res = accountsService.get(externalId, groupId)(hc)
          await(res) shouldBe None

          verify(mockCache, once).get(sid)
          verify(mockOrganisations, once).getOrganisationByGGId(matching(groupId))(any[HeaderCarrier], any[ExecutionContext])
          verify(mockPersons, once).getPerson(matching(externalId))(any[HeaderCarrier], any[ExecutionContext])
          verify(mockCache, never).cache(anyString, any[Accounts])
        }
      }

      "the user has an account, and their account is not cached" should {
        "get the account data from the API and cache it" in {
          val (groupId, externalId): (String, String) = (randomShortString, randomShortString)
          val (person, organisation): (Person, Organisation) = (randomPerson, randomOrganisation)
          val expected = Accounts(organisation.id, person.individualId, organisation, person)

          when(mockOrganisations.getOrganisationByGGId(matching(groupId))(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(organisation)))
          when(mockPersons.getPerson(matching(externalId))(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(person)))

          val res = accountsService.get(externalId, groupId)(hc)
          await(res) shouldBe Some(expected)

          verify(mockCache, once).get(sid)
          verify(mockOrganisations, once).getOrganisationByGGId(matching(groupId))(any[HeaderCarrier], any[ExecutionContext])
          verify(mockPersons, once).getPerson(matching(externalId))(any[HeaderCarrier], any[ExecutionContext])
          verify(mockCache, once).cache(sid, expected)
        }
      }

      "the user's account data is cached" should {
        "get the account data from the cache" in {
          val (groupId, externalId): (String, String) = (randomShortString, randomShortString)
          val (person, organisation): (Person, Organisation) = (randomPerson, randomOrganisation)
          val expected = Accounts(organisation.id, person.individualId, organisation, person)

          when(mockCache.get(sid)).thenReturn(Future.successful(Some(expected)))

          val res = accountsService.get(externalId, groupId)(hc)
          await(res) shouldBe Some(expected)

          verify(mockCache, once).get(sid)
          verify(mockOrganisations, never).getOrganisationByGGId(matching(groupId))(any[HeaderCarrier], any[ExecutionContext])
          verify(mockPersons, never).getPerson(matching(externalId))(any[HeaderCarrier], any[ExecutionContext])
          verify(mockCache, never).cache(anyString, any[Accounts])
        }
      }
    }
  }

  override protected def beforeEach(): Unit = {
    reset(mockOrganisations, mockPersons, mockCache)
    when(mockCache.get(anyString)).thenReturn(Future.successful(None))
    when(mockCache.cache(anyString(), any[Accounts])).thenReturn(Future.successful(()))
  }

  lazy val accountsService = new AccountsService(mockOrganisations, mockPersons, mockCache)(ExecutionContext.Implicits.global)
  lazy val mockOrganisations = mock[OrganisationAccounts]
  lazy val mockPersons = mock[PersonAccounts]
  lazy val mockCache = mock[AccountsCache]

  lazy val once = times(1)
}
