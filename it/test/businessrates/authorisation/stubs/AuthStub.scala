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

package businessrates.authorisation.stubs

import play.api.libs.json.{JsArray, JsObject, Json}

object AuthStub {
  val testExternalId = "testExternalId"
  val testGroupId = "testGroupId"
  val testAffinityGroup = "Individual"

  def hmrcVoaCcaEnrolment(personId: Long): JsObject =
    Json.obj(
      "key" -> "HMRC-VOA-CCA",
      "identifiers" -> Json.arr(
        Json.obj(
          "key"   -> "VOAPersonID",
          "value" -> personId.toString
        )
      )
    )

  def authResponseBody(enrolments: JsObject*): JsObject =
    Json.obj(
      "externalId"      -> testExternalId,
      "groupIdentifier" -> testGroupId,
      "affinityGroup"   -> testAffinityGroup,
      "allEnrolments"   -> JsArray(enrolments)
    )

}
