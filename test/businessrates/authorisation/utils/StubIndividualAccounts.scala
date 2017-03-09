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
import businessrates.authorisation.models.Person
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

object StubIndividualAccounts extends IndividualAccounts(StubHttp) {
  private var stubbedPerson: Option[Person] = None

  def stubPerson(person: Person) = {
    stubbedPerson = Some(person)
  }

  def reset() = {
    stubbedPerson = None
  }

  override def getPerson(externalId: String)(implicit hc: HeaderCarrier): Future[Option[Person]] = Future.successful(stubbedPerson)
}
