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

import play.api.libs.json._

case class GovernmentGatewayDetails(externalId: String, groupId: Option[String], affinityGroup: Option[String]) //TODO make type safe this would allow removal of option and have a default type.

//sealed trait AffinityGroup
//object Organisation extends AffinityGroup
//object Agent extends AffinityGroup
//object Individual extends AffinityGroup
//
//object AffinityGroup {
//
//  val reads: Reads[AffinityGroup] = Reads[AffinityGroup] {
//    case JsString("Organisation") => JsSuccess(Organisation)
//    case JsString("Individual") => JsSuccess(Individual)
//    case JsString("Agent") => JsSuccess(Agent)
//    case value => JsError(s"Affinity group must be a string, of either Organisation, Individual or Agent. $value was provided.")
//  }
//
//  val writes: Writes[AffinityGroup] = Writes[AffinityGroup] {
//    case Organisation => JsString("Organisation")
//    case Individual => JsString("Individual")
//    case Agent => JsString("Agent")
//  }
//
//  implicit val format: Format[AffinityGroup] = Format[AffinityGroup](reads, writes) //Perhaps use this instead of OPtion String
//}