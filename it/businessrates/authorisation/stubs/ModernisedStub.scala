package businessrates.authorisation.stubs

import businessrates.authorisation.{WiremockHelper, WiremockMethods}
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

  def testPersonJson(testExternalId: String): JsValue = Json.parse(s"""
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
        status: Int,
        body: JsValue): StubMapping =
    when(
      method = PATCH,
      uri = s"/customer-management-api/credential/$personId",
      body = Json.obj(
        "GG-Group-ID"    -> groupId,
        "GG-External-ID" -> externalId,
      )
    ).thenReturn(status, body)

}
