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

import businessrates.authorisation.controllers.CacheController
import businessrates.authorisation.repositories.AccountsCache
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.http.HeaderNames

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CacheControllerSpec extends ControllerSpec with MockitoSugar with BeforeAndAfterEach {

  val accountsCache = mock[AccountsCache]

  val controller = new CacheController(accountsCache, stubControllerComponents())

  val sessionId = "session-id-1"

  "CacheController" should {
    "not call accountsCache.drop when sessionID is not in headers" in {
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      controller.clearCache.apply(request)
      verify(accountsCache, times(0)).drop(sessionId)
    }

    "call accountsCache.drop when sessionID exists" in {
      when(accountsCache.drop(anyString())).thenReturn(Future.successful(()))

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(HeaderNames.xSessionId -> sessionId)
      controller.clearCache.apply(request)
      verify(accountsCache, times(1)).drop(sessionId)
    }

  }

}
