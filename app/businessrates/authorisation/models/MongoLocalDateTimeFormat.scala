/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.{Format, Reads, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.{localDateTimeReads, localDateTimeWrites}

import java.time.LocalDateTime

object MongoLocalDateTimeFormat {
  // LocalDateTime must be written to DB as ISODate to allow the expiry TTL on createdOn date to work
  // for data that exists prior to the hmrc-mongo migration -
  final val legacyDateTimeReads: Reads[LocalDateTime] =
    Reads
      .at[String](__)
      .map(dateTime => LocalDateTime.parse(dateTime))

  final implicit val localDateTimeFormat: Format[LocalDateTime] =
    Format(localDateTimeReads.orElse(legacyDateTimeReads), localDateTimeWrites)

}
