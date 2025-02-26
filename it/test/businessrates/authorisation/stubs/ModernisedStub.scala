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
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsValue, Json}

object ModernisedStub extends WiremockMethods with WiremockHelper {
  def testOrgJson(testGroupId: String): JsValue = Json.parse(s"""
                                                                |{
                                                                | "id": 12345,
                                                                | "governmentGatewayGroupId" : "$testGroupId",
                                                                | "organisationLatestDetail": {
                                                                |   "organisationName": "test org",
                                                                |   "addressUnitId": 12,
                                                                |   "organisationEmailAddress": "test@test.com",
                                                                |   "organisationTelephoneNumber": "0123456789",
                                                                |   "representativeFlag": true
                                                                | },
                                                                | "representativeCode": 123456
                                                                |}
                                                                |""".stripMargin)

  def testPersonJson(testExternalId: String): JsValue =
    Json.parse(s"""
                  |{
                  |  "governmentGatewayExternalId": "$testExternalId",
                  |  "organisationId": 12345,
                  |  "id": 12345,
                  |  "personLatestDetail": {
                  |    "firstName": "Testy",
                  |    "lastName": "McTestface",
                  |    "emailAddress": "test@test.com",
                  |    "telephoneNumber": "0123456789",
                  |    "mobileNumber": "0123456789",
                  |    "addressUnitId": 1,
                  |    "identifyVerificationId": "ivId"
                  |  }
                  |}
                  |""".stripMargin)

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

  def stubUpdateCredentials(personId: String, groupId: String, externalId: String)(
        response: ResponseDefinitionBuilder
  ): StubMapping =
    stubFor(
      patch(urlPathEqualTo(s"/customer-management-api/credential/$personId"))
        .withHeader(
          "GG-Group-ID",
          equalTo(groupId)
        )
        .withHeader(
          "GG-External-ID",
          equalTo(externalId)
        )
        .withRequestBody(equalToJson("{}"))
        .willReturn(response)
    )

}
