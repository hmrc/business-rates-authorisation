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

import play.api.libs.json.Format

sealed trait RepresentationStatus extends NamedEnum {
  val name: String
  val key = "propReprStatus"
  override def toString: String = name
}

object RepresentationStatus extends NamedEnumSupport[RepresentationStatus] {
  case object approved extends RepresentationStatus { val name = "APPROVED" }
  case object pending extends RepresentationStatus { val name = "PENDING" }
  case object revoked extends RepresentationStatus { val name = "REVOKED" }
  case object declined extends RepresentationStatus { val name = "DECLINED" }
  case object timedOut extends RepresentationStatus { val name = "TIMED_OUT" }

  implicit val format: Format[RepresentationStatus] = EnumFormat(RepresentationStatus)

  override def all: List[RepresentationStatus] = List(approved, pending, revoked, declined, timedOut)
}
