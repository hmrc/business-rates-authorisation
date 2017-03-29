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

package businessrates.authorisation.models

import play.api.libs.json.{Json, OFormat}

case class PersonDetails(firstName: String, lastName: String, email: String, phone1: String, phone2: Option[String], addressId: Int)

object PersonDetails {
  implicit val formats: OFormat[PersonDetails] = Json.format[PersonDetails]
}

case class Person(externalId: String, trustId: String, organisationId: Long, individualId: Long, details: PersonDetails)

object Person {
  implicit val formats: OFormat[Person] = Json.format[Person]
}
