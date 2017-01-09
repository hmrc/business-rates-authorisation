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

package businessrates.authorisation.utils

import businessrates.authorisation.connectors.GroupAccounts
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

object StubGroupAccounts extends GroupAccounts(StubHttp) {
  private var stubbedOrganisationId: Option[Int] = None

  def stubOrganisationId(organisationId: Int) = {
    stubbedOrganisationId = Some(organisationId)
  }

  def reset() = {
    stubbedOrganisationId = None
  }

  override def getOrganisationId(ggGroupId: String)(implicit hc: HeaderCarrier) = Future.successful(stubbedOrganisationId)
}
