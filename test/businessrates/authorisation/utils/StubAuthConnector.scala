/*
 * Copyright 2018 HM Revenue & Customs
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

import businessrates.authorisation.connectors.AuthConnector
import businessrates.authorisation.controllers.NonEnrolment
import businessrates.authorisation.models.GovernmentGatewayDetails
import businessrates.authorisation.services.AccountsService
import uk.gov.hmrc.play.config.inject.ServicesConfig

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object Mock {
  import org.scalatest.mock.MockitoSugar._

  val servicesConfig = mock[ServicesConfig]
}

object StubAuthConnector extends AuthConnector(StubHttp, Mock.servicesConfig) {
  private var stubGGIds: Option[GovernmentGatewayDetails] = None

  def stubAuthentication(ids: GovernmentGatewayDetails) = {
    stubGGIds = Some(ids)
  }

  def reset() = { stubGGIds = None }

  override def getGovernmentGatewayDetails(implicit hc: HeaderCarrier) = Future.successful(stubGGIds)
}

class StubWithIds(mockAccountService: AccountsService) extends NonEnrolment(StubAuthConnector, mockAccountService)