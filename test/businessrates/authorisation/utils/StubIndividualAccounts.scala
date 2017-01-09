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

import businessrates.authorisation.connectors.IndividualAccounts
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

object StubIndividualAccounts extends IndividualAccounts(StubHttp) {
  private var stubbedPersonId: Option[Int] = None

  def stubPersonId(personId: Int) = {
    stubbedPersonId = Some(personId)
  }

  def reset() = {
    stubbedPersonId = None
  }

  override def getPersonId(externalId: String)(implicit hc: HeaderCarrier) = Future.successful(stubbedPersonId)
}
