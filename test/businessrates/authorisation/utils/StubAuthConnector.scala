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

package businessrates.authorisation.utils

import businessrates.authorisation.controllers.VoaIds
import businessrates.authorisation.services.AccountsService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.config.ServicesConfig

object Mock {

  import org.scalatest.mock.MockitoSugar._

  val servicesConfig = mock[ServicesConfig]
}

class VoaStubWithIds(mockAuthConnector: AuthConnector, mockAccountService: AccountsService) extends VoaIds(mockAuthConnector, mockAccountService)