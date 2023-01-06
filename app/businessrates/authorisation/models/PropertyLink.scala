/*
 * Copyright 2023 HM Revenue & Customs
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

import java.time.{LocalDate, ZoneId}

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Party(organisationId: Long)

object Party {
  private val readsBuilder = (__ \ "authorisedPartyOrganisationId").read[Long]

  implicit val format: OFormat[Party] = OFormat(
    readsBuilder.map(Party.apply),
    Json.writes[Party]
  )
}

case class PropertyLink(
      authorisationId: Long,
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
    def writes(date: LocalDate) = JsNumber(date.atStartOfDay(ZoneId.of("UTC")).toInstant.toEpochMilli)
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

  implicit val format: OFormat[PropertyLink] =
    OFormat(readsBuilder.apply(PropertyLink.apply _), Json.writes[PropertyLink])
}

object PropertyLinkOwnerAndAssessments {
  def unapply(link: Some[PropertyLink]): Option[(Long, Seq[Assessment])] = link.map { l =>
    (l.organisationId, l.assessment)
  }
}

object PropertyLinkAssessmentsAndAgents {
  def unapply(link: Some[PropertyLink]): Option[(Seq[Assessment], Seq[Party])] = link.map { l =>
    (l.assessment, l.agents)
  }
}

object PendingLink {
  def unapply(link: Some[PropertyLink]): Boolean = link.get.pending
}

case class Authorisations(authorisations: Seq[PropertyLink])

object Authorisations {
  implicit val format: OFormat[Authorisations] = Json.format[Authorisations]
}
