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

import org.joda.time.{DateTimeZone, LocalDate}
import play.api.libs.json._
import play.api.libs.functional.syntax._

sealed trait PermissionType extends NamedEnum { override def key = "permissionType" }

case object any extends PermissionType { override def name = "any" }
case object check extends PermissionType { override def name = "check" }
case object challenge extends PermissionType { override def name = "challenge" }

object PermissionType extends NamedEnumSupport[PermissionType] {
  implicit val format = EnumFormat(PermissionType)
  override def all = Seq(check, challenge)
}

case class Permission(checkPermission: AgentPermission, challengePermission: AgentPermission, endDate:Option[LocalDate]) {
  val values: Map[PermissionType, AgentPermission] = Map(check -> checkPermission, challenge -> challengePermission)
}

object Permission {
  private val readsBuilder =
    (__ \ "checkPermission").read[AgentPermission] and
    (__ \ "challengePermission").read[AgentPermission] and
    (__ \ "endDate").readNullable[LocalDate]

  implicit val format: OFormat[Permission] = OFormat(readsBuilder.apply(Permission.apply _), Json.writes[Permission])
}

case class Party(permissions: Seq[Permission], authorisedPartyStatus: RepresentationStatus, organisationId: Long)

object Party {
  private val readsBuilder =
    (__ \ "permissions").read[Seq[Permission]] and
    (__ \ "authorisedPartyStatus").read[RepresentationStatus] and
    (__ \ "authorisedPartyOrganisationId").read[Long]

  implicit val format: OFormat[Party] = OFormat(readsBuilder.apply(Party.apply _), Json.writes[Party])
}

case class PropertyLink(authorisationId: Long,
                        uarn: Long,
                        organisationId: Long,
                        personId: Long,
                        linkedDate: LocalDate,
                        pending: Boolean,
                        assessment: Seq[Assessment],
                        agents: Seq[Party],
                        authorisationStatus: String)

object PropertyLink {
  object ReadsIsPending extends Reads[Boolean] {
    override def reads(json: JsValue): JsResult[Boolean] = JsSuccess(json.as[String] == "PENDING")
  }

  implicit val LocalDateWrites = new Writes[LocalDate] {
    def writes(date: LocalDate) = JsNumber(date.toDateTimeAtStartOfDay(DateTimeZone.UTC).toDate.getTime)
  }

  private val readsBuilder =
    (__ \ "authorisationId").read[Long] and
    (__ \ "uarn").read[Long] and
    (__ \ "authorisationOwnerOrganisationId").read[Long] and
    (__ \ "authorisationOwnerPersonId").read[Long] and
    (__ \ "startDate").read[LocalDate] and
    (__ \ "authorisationStatus").read[Boolean](ReadsIsPending) and
    (__ \ "NDRListValuationHistoryItems").read[Seq[Assessment]] and
    (__ \ "parties").read[Seq[Party]] and
    (__ \ "authorisationStatus").read[String]

  implicit val format: OFormat[PropertyLink] = OFormat(readsBuilder.apply(PropertyLink.apply _), Json.writes[PropertyLink])
}

case class Authorisations(authorisations: Seq[PropertyLink])

object Authorisations {
  implicit val format: OFormat[Authorisations] = Json.format[Authorisations]
}