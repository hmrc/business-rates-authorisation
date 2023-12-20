package businessrates.authorisation.controllers

import businessrates.authorisation.BaseIntegrationSpec
import businessrates.authorisation.stubs.AuthStub.{authResponseBody, hmrcVoaCcaEnrolment}
import businessrates.authorisation.stubs.ModernisedStub._
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.Scenario
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._

class AuthorisationControllerISpec extends BaseIntegrationSpec {
  val testExternalId = "testExternalId"
  val testGroupId = "testGroupId"
  val testPersonId = 12345L
  val testOrgId = 12345L

  val expectedResponseJson: JsObject = Json.obj(
    "organisationId" -> testOrgId,
    "personId"       -> testPersonId,
    "organisation" -> Json.obj(
      "id"          -> testOrgId,
      "groupId"     -> testGroupId,
      "companyName" -> "test org",
      "addressId"   -> 12,
      "email"       -> "test@test.com",
      "phone"       -> "0123456789",
      "isAgent"     -> true,
      "agentCode"   -> 123456
    ),
    "person" -> Json.obj(
      "externalId"     -> testExternalId,
      "trustId"        -> "ivId",
      "organisationId" -> testOrgId,
      "individualId"   -> testPersonId,
      "details" -> Json.obj(
        "firstName" -> "Testy",
        "lastName"  -> "McTestface",
        "email"     -> "test@test.com",
        "phone1"    -> "0123456789",
        "phone2"    -> "0123456789",
        "addressId" -> 1
      )
    )
  )

  "GET /authenticate" when {
    "the user has an existing account on modernised" should {

      "return OK with the user's IDs" in {
        stubFor(
          post("/auth/authorise")
            .willReturn(okJson(authResponseBody().toString))
        )

        stubFor(
          get(urlPathEqualTo(s"/customer-management-api/organisation"))
            .withQueryParam("governmentGatewayGroupId", equalTo(testGroupId))
            .willReturn(okJson(testOrgJson(testGroupId).toString))
        )

        stubFor(
          get(urlPathEqualTo(s"/customer-management-api/person"))
            .withQueryParam("governmentGatewayExternalId", equalTo(testExternalId))
            .willReturn(okJson(testPersonJson(testExternalId).toString))
        )

        val res = await(ws.url(s"$baseUrl/authenticate").withHttpHeaders("AUTHORIZATION" -> "testBearerToken").get())

        res.status shouldBe OK
        res.json shouldBe expectedResponseJson
      }
    }
    "the user does not have an existing organisation account on modernised" when {
      "the user has an HMRC-VOA-CCA enrolment" should {
        "return OK with the user's IDs from their person ID" in {
          val credentialMigration = "Credential Migration"
          val credentialMigrated = "Credential migrated"

          stubFor(
            post("/auth/authorise")
              .willReturn(okJson(authResponseBody(hmrcVoaCcaEnrolment(testPersonId)).toString))
          )

          stubFor(
            get(urlPathEqualTo(s"/customer-management-api/organisation"))
              .inScenario(credentialMigration)
              .whenScenarioStateIs(Scenario.STARTED)
              .withQueryParam("governmentGatewayGroupId", equalTo(testGroupId))
              .willReturn(notFound)
          )

          stubFor(
            get(urlPathEqualTo(s"/customer-management-api/person"))
              .inScenario(credentialMigration)
              .whenScenarioStateIs(Scenario.STARTED)
              .withQueryParam("governmentGatewayExternalId", equalTo(testExternalId))
              .willReturn(notFound)
          )

          stubFor(
            patch(urlPathEqualTo(s"/customer-management-api/credential/$testPersonId"))
              .inScenario(credentialMigration)
              .withRequestBody(
                equalToJson(
                  Json
                    .obj(
                      "GG-Group-ID"    -> testGroupId,
                      "GG-External-ID" -> testExternalId,
                    )
                    .toString()))
              .willReturn(ok)
              .willSetStateTo(credentialMigrated)
          )

          stubFor(
            get(urlPathEqualTo(s"/customer-management-api/organisation"))
              .inScenario(credentialMigration)
              .whenScenarioStateIs(credentialMigrated)
              .withQueryParam("governmentGatewayGroupId", equalTo(testGroupId))
              .willReturn(okJson(testOrgJson(testGroupId).toString))
          )

          stubFor(
            get(urlPathEqualTo(s"/customer-management-api/person"))
              .inScenario(credentialMigration)
              .whenScenarioStateIs(credentialMigrated)
              .withQueryParam("governmentGatewayExternalId", equalTo(testExternalId))
              .willReturn(okJson(testPersonJson(testExternalId).toString))
          )

          val res = await(
            ws.url(s"$baseUrl/authenticate")
              .withHttpHeaders("AUTHORIZATION" -> "testBearerToken")
              .get())

          res.status shouldBe OK
          res.json shouldBe expectedResponseJson

        }
      }
      "the user does not have an HMRC-VOA-CCA enrolment" should {
        "return UNAUTHORIZED" in {
          stubFor(
            post("/auth/authorise")
              .willReturn(okJson(authResponseBody().toString))
          )

          stubFor(
            get(urlPathEqualTo(s"/customer-management-api/organisation"))
              .withQueryParam("governmentGatewayGroupId", equalTo(testGroupId))
              .willReturn(notFound)
          )

          val res = await(ws.url(s"$baseUrl/authenticate").withHttpHeaders("AUTHORIZATION" -> "testBearerToken").get())

          res.status shouldBe UNAUTHORIZED
          res.json shouldBe Json.obj(
            "errorCode" -> "NO_CUSTOMER_RECORD"
          )
        }
      }
    }

    "the user is part of an existing organisation but does not match to a person" should {
      "return UNAUTHORIZED" in {
        stubFor(
          post("/auth/authorise")
            .willReturn(okJson(authResponseBody().toString))
        )

        stubFor(
          get(urlPathEqualTo(s"/customer-management-api/organisation"))
            .withQueryParam("governmentGatewayGroupId", equalTo(testGroupId))
            .willReturn(okJson(testOrgJson(testGroupId).toString))
        )

        stubFor(
          get(urlPathEqualTo(s"/customer-management-api/person"))
            .withQueryParam("governmentGatewayExternalId", equalTo(testExternalId))
            .willReturn(notFound)
        )

        val res = await(ws.url(s"$baseUrl/authenticate").withHttpHeaders("AUTHORIZATION" -> "testBearerToken").get())

        res.status shouldBe UNAUTHORIZED
        res.json shouldBe Json.obj(
          "errorCode" -> "NO_CUSTOMER_RECORD"
        )
      }
    }
  }
}
