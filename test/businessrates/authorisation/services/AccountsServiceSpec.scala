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

package businessrates.authorisation.services

import akka.util.Timeout
import businessrates.authorisation.ArbitraryDataGeneration
import businessrates.authorisation.config.FeatureSwitch
import businessrates.authorisation.connectors.BackendConnector.UpdateCredentialsSuccess
import businessrates.authorisation.connectors.{BstBackendConnector, ModernisedBackendConnector}
import businessrates.authorisation.models.{Accounts, Organisation, Person}
import businessrates.authorisation.repositories.AccountsCache
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.mockito.verification.VerificationMode
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.await
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AccountsServiceSpec
    extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach with ArbitraryDataGeneration {

  implicit val timeout: Timeout = Timeout(Span(2500, Millis))
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  trait TestSetup {
    val mockModernisedConnector: ModernisedBackendConnector = mock[ModernisedBackendConnector]
    val mockBstConnector: BstBackendConnector = mock[BstBackendConnector]
    val mockCache: AccountsCache = mock[AccountsCache]
    val mockFeatureSwitch: FeatureSwitch = mock[FeatureSwitch]

    val accountsService =
      new AccountsService(mockModernisedConnector, mockBstConnector, mockCache, mockFeatureSwitch)

    when(mockCache.get(anyString)).thenReturn(Future.successful(None))
    when(mockCache.cache(anyString(), any[Accounts])).thenReturn(Future.successful(()))
  }

  val emptyEnrolments = Enrolments(Set.empty)

  "Accounts service with the BST Downstream enabled" when {
    "the request does not have a session id" should {
      val organisation: Organisation = randomOrganisation
      val person: Person = randomPerson

      "get the account data from the API and not cache it" in new TestSetup {
        val (groupId, externalId): (String, String) = (randomShortString, randomShortString)
        val expected: Accounts = Accounts(organisation.id, person.individualId, organisation, person)

        when(mockBstConnector.getOrganisationByGGId(anyString())(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Some(organisation)))
        when(mockBstConnector.getPerson(anyString())(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Some(person)))
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)

        val res: Option[Accounts] = await(accountsService.get(externalId, groupId, emptyEnrolments)(HeaderCarrier()))
        res shouldBe Some(expected)

        verify(mockBstConnector, once).getOrganisationByGGId(matching(groupId))(
          any[HeaderCarrier],
          any[ExecutionContext])
        verify(mockBstConnector, once).getPerson(matching(externalId))(any[HeaderCarrier], any[ExecutionContext])
        verify(mockCache, never).get(anyString())
        verify(mockCache, never).cache(anyString, matching(expected))
      }
    }

    "the request does have a session id" when {
      val sid = UUID.randomUUID().toString
      val hc = HeaderCarrier(sessionId = Some(SessionId(sid)))

      "the user does not have an account" should {
        "not cache the result" in new TestSetup {
          val (groupId, externalId): (String, String) = (randomShortString, randomShortString)

          when(mockBstConnector.getOrganisationByGGId(anyString())(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(None))
          when(mockBstConnector.getPerson(anyString())(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(None))
          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)

          val res: Option[Accounts] = await(accountsService.get(externalId, groupId, emptyEnrolments)(hc))
          res shouldBe None

          verify(mockCache, once).get(sid)
          verify(mockBstConnector).getOrganisationByGGId(matching(groupId))(any[HeaderCarrier], any[ExecutionContext])
          verify(mockBstConnector, never).getPerson(matching(externalId))(any[HeaderCarrier], any[ExecutionContext])
          verify(mockCache, never).cache(anyString, any[Accounts])
        }
      }

      "the user has an account, and their account is not cached" should {
        "get the account data from the API and cache it" in new TestSetup {
          val (groupId, externalId): (String, String) = (randomShortString, randomShortString)
          val (person, organisation): (Person, Organisation) = (randomPerson, randomOrganisation)
          val expected: Accounts = Accounts(organisation.id, person.individualId, organisation, person)

          when(mockBstConnector.getOrganisationByGGId(matching(groupId))(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some(organisation)))
          when(mockBstConnector.getPerson(matching(externalId))(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some(person)))
          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)

          val res: Option[Accounts] = await(accountsService.get(externalId, groupId, emptyEnrolments)(hc))
          res shouldBe Some(expected)

          verify(mockCache, once).get(sid)
          verify(mockBstConnector, once).getOrganisationByGGId(matching(groupId))(
            any[HeaderCarrier],
            any[ExecutionContext])
          verify(mockBstConnector, once).getPerson(matching(externalId))(any[HeaderCarrier], any[ExecutionContext])
          verify(mockCache, once).cache(sid, expected)
        }
      }

      "the user's account data is cached" should {
        "get the account data from the cache" in new TestSetup {
          val (groupId, externalId): (String, String) = (randomShortString, randomShortString)
          val (person, organisation): (Person, Organisation) = (randomPerson, randomOrganisation)
          val expected: Accounts = Accounts(organisation.id, person.individualId, organisation, person)

          when(mockCache.get(sid)).thenReturn(Future.successful(Some(expected)))

          val res: Option[Accounts] = await(accountsService.get(externalId, groupId, emptyEnrolments)(hc))
          res shouldBe Some(expected)

          verify(mockCache, once).get(sid)
          verify(mockBstConnector, never).getOrganisationByGGId(matching(groupId))(
            any[HeaderCarrier],
            any[ExecutionContext])
          verify(mockBstConnector, never).getPerson(matching(externalId))(any[HeaderCarrier], any[ExecutionContext])
          verify(mockCache, never).cache(anyString, any[Accounts])
        }
      }
    }

  }

  "Accounts service with the BST Downstream disabled" when {
    "the request does not have a session id" should {
      val organisation: Organisation = randomOrganisation
      val person: Person = randomPerson

      "get the account data from the API and not cache it" in new TestSetup {
        val (groupId, externalId): (String, String) = (randomShortString, randomShortString)
        val expected: Accounts = Accounts(organisation.id, person.individualId, organisation, person)

        when(mockModernisedConnector.getOrganisationByGGId(anyString())(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Some(organisation)))
        when(mockModernisedConnector.getPerson(anyString())(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Some(person)))
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(false)

        val res = await(accountsService.get(externalId, groupId, emptyEnrolments)(HeaderCarrier()))
        res shouldBe Some(expected)

        verify(mockModernisedConnector, once).getOrganisationByGGId(matching(groupId))(
          any[HeaderCarrier],
          any[ExecutionContext])
        verify(mockModernisedConnector, once).getPerson(matching(externalId))(any[HeaderCarrier], any[ExecutionContext])
        verify(mockCache, never).get(anyString())
        verify(mockCache, never).cache(anyString, matching(expected))
      }
    }

    "the request does have a session id" when {
      val sid = UUID.randomUUID().toString
      val hc = HeaderCarrier(sessionId = Some(SessionId(sid)))

      "the user does not have an account" when {
        "the user has an HMRC-VOA-CCA enrolment" should {
          "call to update the backend with the new groupId and externalId" in new TestSetup {
            val (groupId, externalId): (String, String) = (randomShortString, randomShortString)
            val (person, organisation): (Person, Organisation) = (randomPerson, randomOrganisation)
            val expected: Accounts = Accounts(organisation.id, person.individualId, organisation, person)

            val testEnrolments = Enrolments(
              Set(
                Enrolment(
                  key = "HMRC-VOA-CCA",
                  identifiers = Seq(EnrolmentIdentifier("VOAPersonID", person.individualId.toString)),
                  state = "activated"
                )))

            when(mockModernisedConnector.getOrganisationByGGId(anyString())(any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(None), Future.successful(Some(organisation)))

            when(mockModernisedConnector.getPerson(anyString())(any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(Some(person)))

            when(
              mockModernisedConnector.updateCredentials(
                ArgumentMatchers.eq(person.individualId.toString),
                ArgumentMatchers.eq(groupId),
                ArgumentMatchers.eq(externalId))(any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(UpdateCredentialsSuccess))

            when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(false)

            val res: Option[Accounts] = await(accountsService.get(externalId, groupId, testEnrolments)(hc))
            res shouldBe Some(expected)

            verify(mockCache, once).get(sid)
            verify(mockModernisedConnector, times(2))
              .getOrganisationByGGId(matching(groupId))(any[HeaderCarrier], any[ExecutionContext])
            verify(mockModernisedConnector, once).getPerson(matching(externalId))(
              any[HeaderCarrier],
              any[ExecutionContext])
            verify(mockModernisedConnector, once).updateCredentials(
              matching(person.individualId.toString),
              matching(groupId),
              matching(externalId),
            )(
              any[HeaderCarrier],
              any[ExecutionContext]
            )
            verify(mockCache, once).cache(anyString, matching(expected))
          }
        }
        "the user does not have an HMRC-VOA-CCA enrolment" should {
          "not cache the result" in new TestSetup {
            val (groupId, externalId): (String, String) = (randomShortString, randomShortString)

            when(mockModernisedConnector.getOrganisationByGGId(anyString())(any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(None))
            when(mockModernisedConnector.getPerson(anyString())(any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(None))
            when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(false)

            val res: Option[Accounts] = await(accountsService.get(externalId, groupId, emptyEnrolments)(hc))
            res shouldBe None

            verify(mockCache, once).get(sid)
            verify(mockModernisedConnector).getOrganisationByGGId(matching(groupId))(
              any[HeaderCarrier],
              any[ExecutionContext])
            verify(mockModernisedConnector, never).getPerson(matching(externalId))(
              any[HeaderCarrier],
              any[ExecutionContext])
            verify(mockCache, never).cache(anyString, any[Accounts])
          }
        }
      }

      "the user has an account, and their account is not cached" should {
        "get the account data from the API and cache it" in new TestSetup {
          val (groupId, externalId): (String, String) = (randomShortString, randomShortString)
          val (person, organisation): (Person, Organisation) = (randomPerson, randomOrganisation)
          val expected: Accounts = Accounts(organisation.id, person.individualId, organisation, person)

          when(
            mockModernisedConnector.getOrganisationByGGId(matching(groupId))(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some(organisation)))
          when(mockModernisedConnector.getPerson(matching(externalId))(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some(person)))
          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(false)

          val res = await(accountsService.get(externalId, groupId, emptyEnrolments)(hc))
          res shouldBe Some(expected)

          verify(mockCache, once).get(sid)
          verify(mockModernisedConnector, once).getOrganisationByGGId(matching(groupId))(
            any[HeaderCarrier],
            any[ExecutionContext])
          verify(mockModernisedConnector, once).getPerson(matching(externalId))(
            any[HeaderCarrier],
            any[ExecutionContext])
          verify(mockCache, once).cache(sid, expected)
        }
      }

      "the user's account data is cached" should {
        "get the account data from the cache" in new TestSetup {
          val (groupId, externalId): (String, String) = (randomShortString, randomShortString)
          val (person, organisation): (Person, Organisation) = (randomPerson, randomOrganisation)
          val expected: Accounts = Accounts(organisation.id, person.individualId, organisation, person)

          when(mockCache.get(sid)).thenReturn(Future.successful(Some(expected)))

          val res = await(accountsService.get(externalId, groupId, emptyEnrolments)(hc))
          res shouldBe Some(expected)

          verify(mockCache, once).get(sid)
          verify(mockModernisedConnector, never).getOrganisationByGGId(matching(groupId))(
            any[HeaderCarrier],
            any[ExecutionContext])
          verify(mockModernisedConnector, never).getPerson(matching(externalId))(
            any[HeaderCarrier],
            any[ExecutionContext])
          verify(mockCache, never).cache(anyString, any[Accounts])
        }
      }
    }
  }

  lazy val once: VerificationMode = times(1)
}
