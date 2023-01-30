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

import businessrates.authorisation.repositories.{AccountsCache, AccountsMongoCache}
import businessrates.authorisation.services.jobHandler.AccountsCacheJobHandler
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout, running}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountsCacheJobHandlerSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  "AccountsCacheJobHandler process job" should {
    "run updateStringCreatedAtTimestamp" in new Setup {
      when(mockAccountsCache.updateStringCreatedAtTimestamp).thenReturn(Future.successful(20L))
      running(app) {

        await(handler.processJob())

        verify(mockAccountsCache).updateStringCreatedAtTimestamp
      }
    }
  }

  trait Setup {

    val app: Application = GuiceApplicationBuilder()
      .overrides()
      .configure(
        "microservice.metrics.enabled" -> false,
        "metrics.enabled"              -> false,
        "auditing.enabled"             -> false,
        "housekeepingIntervalMinutes"  -> 1
      )
      .build()

    val mockAccountsCache: AccountsMongoCache = mock[AccountsMongoCache]
    val handler = new AccountsCacheJobHandler(mockAccountsCache)

  }
}
