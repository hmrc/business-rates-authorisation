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

import businessrates.authorisation.{WiremockHelper, WiremockMethods}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsValue

object BstStub extends WiremockMethods with WiremockHelper {

  def stubGetOrganisationByGGId(ggId: String)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"/customer-management-api/organisation\\?governmentGatewayGroupId=$ggId"
    ).thenReturn(status, body)

  def stubGetOrganisationByOrgId(orgId: Long)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"/customer-management-api/organisation\\?organisationId=$orgId"
    ).thenReturn(status, body)

  def stubGetPerson(externalId: String)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"/customer-management-api/person\\?governmentGatewayExternalId=$externalId"
    ).thenReturn(status, body)

}
