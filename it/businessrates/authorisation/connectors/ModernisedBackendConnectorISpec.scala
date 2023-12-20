package businessrates.authorisation.connectors

import businessrates.authorisation.BaseIntegrationSpec
import businessrates.authorisation.connectors.BackendConnector.UpdateCredentialsSuccess
import businessrates.authorisation.models.{Organisation, Person, PersonDetails}
import businessrates.authorisation.stubs.ModernisedStub
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.ExecutionContext

class ModernisedBackendConnectorISpec extends BaseIntegrationSpec {
  import ModernisedStub._

  lazy val connector: ModernisedBackendConnector = app.injector.instanceOf[ModernisedBackendConnector]

  trait TestSetup {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrier()

  }

  "getOrganisationByGGId" should {
    "return the correct data model" when {
      "supplied the correct parameters" in new TestSetup {

        val testGgId = "test-gg-id"

        val testOrgJson: JsValue = Json.parse(s"""
                                                 |{
                                                 | "id": 12345,
                                                 | "governmentGatewayGroupId" : "$testGgId",
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

        val testOrg: Organisation =
          Organisation(
            id = 12345,
            groupId = "test-gg-id",
            companyName = "test org",
            addressId = 12,
            email = "test@test.com",
            phone = "0123456789",
            isAgent = true,
            agentCode = Some(123456)
          )

        stubGetOrganisationByGGId(testGgId)(OK, testOrgJson)

        val result: Option[Organisation] = await(connector.getOrganisationByGGId(testGgId))

        result shouldBe Some(testOrg)
      }
    }
    "return None" when {
      "a 404 (Not Found) is returned" in new TestSetup {

        val testGgId = "test-gg-id"

        val result: Option[Organisation] = await(connector.getOrganisationByGGId(testGgId))

        result shouldBe None
      }
    }
  }

  "getPerson" should {
    "return the correct data model" when {
      "supplied the correct parameters" in new TestSetup {

        val testExternalId: String = "testExternalId"

        val testPersonJson: JsValue = Json.parse(s"""
                                                    |{
                                                    |  "governmentGatewayExternalId": "$testExternalId",
                                                    |  "organisationId": 123456,
                                                    |  "id": 123456,
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

        val testPerson: Person = Person(
          externalId = testExternalId,
          trustId = "ivId",
          organisationId = 123456L,
          individualId = 123456L,
          details = PersonDetails(
            firstName = "Testy",
            lastName = "McTestface",
            email = "test@test.com",
            phone1 = "0123456789",
            phone2 = Some("0123456789"),
            addressId = 1
          )
        )

        stubGetPerson(testExternalId)(OK, testPersonJson)

        val result: Option[Person] = await(connector.getPerson(testExternalId))

        result shouldBe Some(testPerson)
      }

      "return None" when {
        "a 404 (Not Found) is returned" in new TestSetup {

          val testExternalId: String = "testExternalId"

          val result: Option[Person] = await(connector.getPerson(testExternalId))

          result shouldBe None
        }
      }
    }
  }

  "updateCredentials" should {
    "return a success" when {
      "Modernised returns a success" in new TestSetup {
        val testPersonId = "testPersonId"
        val testExternalId = "testExternalId"
        val testGroupId = "testGroupId"

        stubUpdateCredentials(personId = testPersonId, externalId = testExternalId, groupId = testGroupId)(
          OK,
          Json.obj())

        val result: BackendConnector.UpdateCredentialsSuccess.type =
          await(connector.updateCredentials(testPersonId, testGroupId, testExternalId))

        result shouldBe UpdateCredentialsSuccess
      }
    }
    "return a failed future" when {
      "Modernised returns anything else" in new TestSetup {
        val testPersonId = "testPersonId"
        val testExternalId = "testExternalId"
        val testGroupId = "testGroupId"

        stubUpdateCredentials(personId = testPersonId, externalId = testExternalId, groupId = testGroupId)(
          NOT_FOUND,
          Json.obj())

        intercept[NotFoundException](await(connector.updateCredentials(testPersonId, testGroupId, testExternalId)))

      }
    }
  }
}
