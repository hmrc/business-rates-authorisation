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

package businessrates.authorisation.models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class PersonDetails(
      firstName: String,
      lastName: String,
      email: String,
      phone1: String,
      phone2: Option[String],
      addressId: Int)
case class Person(externalId: String, trustId: String, organisationId: Long, individualId: Long, details: PersonDetails)

object PersonDetails {
  val apiFormat: Reads[PersonDetails] = (
    (__ \ "firstName").read[String] and
      (__ \ "lastName").read[String] and
      (__ \ "emailAddress").read[String] and
      (__ \ "telephoneNumber").read[String] | Reads.pure("not set") and
      (__ \ "mobileNumber").readNullable[String] and
      (__ \ "addressUnitId").read[Int]
  )(PersonDetails.apply _)

  implicit val format: OFormat[PersonDetails] = Json.format[PersonDetails]
}

object Person {
  val apiFormat: Reads[Person] = (
    (__ \ "governmentGatewayExternalId").read[String] and
      (__ \ "personLatestDetail" \ "identifyVerificationId").read[String] | Reads.pure("") and
      (__ \ "organisationId").read[Long] and
      (__ \ "id").read[Long] and
      (__ \ "personLatestDetail").read[PersonDetails](PersonDetails.apiFormat)
  )(Person.apply _)

  implicit val format: OFormat[Person] = Json.format[Person]
}
