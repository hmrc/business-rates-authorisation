/*
 * Copyright 2021 HM Revenue & Customs
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

import businessrates.authorisation.utils.{StubOrganisationAccounts, StubPersonAccounts, StubPropertyLinking}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}

class ControllerSpec
    extends AnyWordSpec with Matchers with BeforeAndAfterEach with FutureAwaits with DefaultAwaitTimeout
    with ArbitraryDataGeneration {

  override protected def beforeEach() = {
    StubOrganisationAccounts.reset()
    StubPersonAccounts.reset()
    StubPropertyLinking.reset()
  }

  implicit val request = FakeRequest()
}
