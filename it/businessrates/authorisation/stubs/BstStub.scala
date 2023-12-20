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
