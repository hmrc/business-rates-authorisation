/*
 * Copyright 2026 HM Revenue & Customs
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
import businessrates.authorisation.models.{Accounts, Organisation, Person}
import businessrates.authorisation.services.AccountsService
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, UNAUTHORIZED, contentAsJson, status, stubControllerComponents}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthorisationControllerSpec extends ControllerSpec with MockitoSugar {

  type AuthRetrievals = Option[String] ~ Option[String] ~ Option[AffinityGroup] ~ Enrolments

  val testExternalId: String = randomShortString
  val testGroupId: String = randomShortString
  val organisation: Organisation = randomOrganisation
  val person: Person = randomPerson
  val accounts: Accounts = Accounts(organisation.id, person.individualId, organisation, person)

  val ccaEnrolments: Enrolments = Enrolments(
    Set(Enrolment("HMRC-VOA-CCA", Seq(EnrolmentIdentifier("VOAPersonID", "12345")), "Activated"))
  )

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  def authenticatedJson(accounts: Accounts): JsObject =
    Json.obj("authenticated" -> true, "accounts" -> Json.toJson(accounts))

  def notAuthenticatedJson(errorCode: String): JsObject =
    Json.obj("authenticated" -> false, "errorCode" -> errorCode)

  trait TestSetup {
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockAccountsService: AccountsService = mock[AccountsService]

    val controller = new AuthorisationController(mockAccountsService, stubControllerComponents(), mockAuthConnector)

    def givenAuthRetrieves(
          externalId: Option[String] = Some(testExternalId),
          groupId: Option[String] = Some(testGroupId),
          affinityGroup: Option[AffinityGroup] = Some(Individual),
          enrolments: Enrolments = Enrolments(Set.empty)
    ): Unit = {
      val retrievals: AuthRetrievals = new ~(new ~(new ~(externalId, groupId), affinityGroup), enrolments)
      when(
        mockAuthConnector.authorise(any[Predicate](), any[Retrieval[AuthRetrievals]]())(
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(Future.successful(retrievals))
    }

    def givenAuthFails(exception: Throwable): Unit =
      when(
        mockAuthConnector.authorise(any[Predicate](), any[Retrieval[AuthRetrievals]]())(
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(Future.failed(exception))

    def givenAccounts(result: Option[Accounts]): Unit =
      when(mockAccountsService.get(anyString(), anyString(), any[Enrolments]())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(result))

    def verifyAccountsServiceNotCalled(): Unit =
      verify(mockAccountsService, never).get(anyString(), anyString(), any[Enrolments]())(any[HeaderCarrier]())
  }

  "AuthorisationController.authenticate" should {
    Seq(Individual, AffinityGroup.Organisation).foreach { affinityGroup =>
      s"return OK with the user's accounts when the user has a $affinityGroup account and a customer record" in new TestSetup {
        givenAuthRetrieves(affinityGroup = Some(affinityGroup))
        givenAccounts(Some(accounts))

        val result = controller.authenticate(request)

        status(result) shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(accounts)
      }
    }

    "return UNAUTHORIZED with NO_CUSTOMER_RECORD when the user has no customer record" in new TestSetup {
      givenAuthRetrieves()
      givenAccounts(None)

      val result = controller.authenticate(request)

      status(result) shouldBe UNAUTHORIZED
      contentAsJson(result) shouldBe Json.obj("errorCode" -> "NO_CUSTOMER_RECORD")
    }

    "return UNAUTHORIZED with INVALID_ACCOUNT_TYPE when the user has a non-permitted affinity group" in new TestSetup {
      givenAuthRetrieves(affinityGroup = Some(Agent))

      val result = controller.authenticate(request)

      status(result) shouldBe UNAUTHORIZED
      contentAsJson(result) shouldBe Json.obj("errorCode" -> "INVALID_ACCOUNT_TYPE")
      verifyAccountsServiceNotCalled()
    }

    "return UNAUTHORIZED with INVALID_GATEWAY_SESSION when auth does not return the required retrievals" in new TestSetup {
      givenAuthRetrieves(externalId = None)

      val result = controller.authenticate(request)

      status(result) shouldBe UNAUTHORIZED
      contentAsJson(result) shouldBe Json.obj("errorCode" -> "INVALID_GATEWAY_SESSION")
      verifyAccountsServiceNotCalled()
    }

    "return UNAUTHORIZED with INVALID_GATEWAY_SESSION when the user is not logged in" in new TestSetup {
      givenAuthFails(MissingBearerToken())

      val result = controller.authenticate(request)

      status(result) shouldBe UNAUTHORIZED
      contentAsJson(result) shouldBe Json.obj("errorCode" -> "INVALID_GATEWAY_SESSION")
      verifyAccountsServiceNotCalled()
    }

    "propagate unexpected exceptions" in new TestSetup {
      givenAuthFails(new RuntimeException("auth unavailable"))

      a[RuntimeException] should be thrownBy await(controller.authenticate(request))
    }
  }

  "AuthorisationController.authenticationStatus" should {
    Seq(Individual, AffinityGroup.Organisation).foreach { affinityGroup =>
      s"return OK with authenticated true and the user's accounts when the user has a $affinityGroup account and a customer record" in new TestSetup {
        givenAuthRetrieves(affinityGroup = Some(affinityGroup))
        givenAccounts(Some(accounts))

        val result = controller.authenticationStatus(request)

        status(result) shouldBe OK
        contentAsJson(result) shouldBe authenticatedJson(accounts)
      }
    }

    "pass the retrieved identifiers and enrolments to the accounts service" in new TestSetup {
      givenAuthRetrieves(enrolments = ccaEnrolments)
      givenAccounts(Some(accounts))

      await(controller.authenticationStatus(request))

      verify(mockAccountsService, times(1))
        .get(matching(testExternalId), matching(testGroupId), matching(ccaEnrolments))(any[HeaderCarrier]())
    }

    "return OK with authenticated false and errorCode NO_CUSTOMER_RECORD when the user has no customer record" in new TestSetup {
      givenAuthRetrieves()
      givenAccounts(None)

      val result = controller.authenticationStatus(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe notAuthenticatedJson("NO_CUSTOMER_RECORD")
    }

    "return OK with authenticated false and errorCode INVALID_ACCOUNT_TYPE when the user has a non-permitted affinity group" in new TestSetup {
      givenAuthRetrieves(affinityGroup = Some(Agent))

      val result = controller.authenticationStatus(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe notAuthenticatedJson("INVALID_ACCOUNT_TYPE")
      verifyAccountsServiceNotCalled()
    }

    "return OK with authenticated false and errorCode INVALID_GATEWAY_SESSION when auth does not return the required retrievals" in new TestSetup {
      givenAuthRetrieves(groupId = None)

      val result = controller.authenticationStatus(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe notAuthenticatedJson("INVALID_GATEWAY_SESSION")
      verifyAccountsServiceNotCalled()
    }

    "return OK with authenticated false and errorCode INVALID_GATEWAY_SESSION when the user is not logged in" in new TestSetup {
      givenAuthFails(MissingBearerToken())

      val result = controller.authenticationStatus(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe notAuthenticatedJson("INVALID_GATEWAY_SESSION")
      verifyAccountsServiceNotCalled()
    }

    "return OK with authenticated false and errorCode INVALID_GATEWAY_SESSION when the user's session has expired" in new TestSetup {
      givenAuthFails(BearerTokenExpired())

      val result = controller.authenticationStatus(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe notAuthenticatedJson("INVALID_GATEWAY_SESSION")
    }

    "propagate unexpected exceptions" in new TestSetup {
      givenAuthFails(new RuntimeException("auth unavailable"))

      a[RuntimeException] should be thrownBy await(controller.authenticationStatus(request))
    }
  }
}
