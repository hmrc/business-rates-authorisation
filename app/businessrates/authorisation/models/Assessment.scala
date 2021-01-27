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

import java.time.LocalDate
import play.api.libs.json.{Json, OFormat}
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Assessment(assessmentRef: Long, listYear: String, uarn: Long, effectiveDate: LocalDate)

object Assessment {
  private val readsBuilder =
    (__ \ "asstRef").read[Long] and
      (__ \ "listYear").read[String] and
      (__ \ "uarn").read[Long] and
      (__ \ "effectiveDate").read[LocalDate]

  implicit val formats: OFormat[Assessment] = OFormat(readsBuilder.apply(Assessment.apply _), Json.writes[Assessment])
}
