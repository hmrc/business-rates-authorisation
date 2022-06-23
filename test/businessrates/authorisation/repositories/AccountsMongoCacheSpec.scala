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

package businessrates.authorisation.repositories

import akka.util.Timeout
import businessrates.authorisation.models.{Accounts, Organisation, Person, PersonDetails}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.await
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.ExecutionContext.Implicits.global

class AccountsMongoCacheSpec extends AnyWordSpec with Matchers with ScalaFutures with GuiceOneAppPerSuite {
  implicit val timeout: Timeout = Timeout(Span(250, Millis))

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  val db: MongoComponent = app.injector.instanceOf[MongoComponent]

  private val accountsMongoCache = new AccountsMongoCache(db)

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure("mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}")

  private val addressId = 1234
  private val organisationId = 1234
  private val individualId = 5678
  private val agentCode = 546345L
  private val isAgent = false
  private val personDetails = PersonDetails("FirstName", "LastName", "email@email.com", "0123456789", None, addressId)
  private val person = Person("govGatewayId", "trustId", organisationId, individualId, personDetails)
  private val organisation = Organisation(
    organisationId,
    "groupId",
    "companyName",
    addressId,
    "email@test.com",
    "0213456788",
    isAgent,
    Some(agentCode).filter(_ => isAgent))
  private val accounts = Accounts(organisationId, 57654, organisation, person)

  "AccountsMongoCacheSpec" should {

    "cache an Account, retrieve and drop the Accounts record" in {

      val sessionId = "session-id-1"
      await(accountsMongoCache.cache(sessionId, accounts))

      val maybeAccounts = await(accountsMongoCache.get(sessionId))

      maybeAccounts.isDefined shouldBe true
      maybeAccounts.get shouldBe accounts

      await(accountsMongoCache.drop(sessionId))

      val maybeDroppedAccounts = await(accountsMongoCache.get(sessionId))
      maybeDroppedAccounts.isDefined shouldBe false

    }

  }

}
