/*
 * Copyright 2017 HM Revenue & Customs
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

package businessrates.authorisation.connectors

import businessrates.authorisation.models.{Organisation, Person, PersonDetails}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import play.api.libs.json.{JsSuccess, Json}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpReads}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BackendConnectorSpec extends WordSpec with MustMatchers with MockitoSugar with BeforeAndAfterEach
  with FutureAwaits with DefaultAwaitTimeout {

  private val inputOrgJson =
    """{
      |"id":1000000003,
      |"governmentGatewayGroupId":"stub-group-3",
      |"representativeCode":990551132,
      |"organisationLatestDetail":
      |{
      |	"addressUnitId":1000000000,
      |	"representativeFlag":false,
      |	"smallBusinessFlag":true,
      |	"organisationName":"Automated Stub 3",
      |	"organisationEmailAddress":"stub3@voa.gov.uk",
      |	"organisationTelephoneNumber":"0123456783",
      |	"effectiveFrom":"2017-02-16"
      |},
      |"persons":[
      |	{"personLatestDetail":{"id":846189244,"addressUnitId":-1815520542,"firstName":"Jim","lastName":"James","emailAddress":"jj@example.com","telephoneNumber":"124567890","identifyVerificationId":"ecd6d989-05a5-4c06-8528-055d46ab5ce6","effectiveFrom":"2017-03-30"}},
      |	{"personLatestDetail":{"id":619144174,"addressUnitId":-1674062666,"firstName":"Jim","lastName":"James","emailAddress":"jj@example.com","telephoneNumber":"124567890","identifyVerificationId":"d1e765fa-f208-4530-b634-fe366ec5984b","effectiveFrom":"2017-03-31"}},
      |	{"personLatestDetail":{"id":1802901214,"addressUnitId":1160458860,"firstName":"Jim","lastName":"James","emailAddress":"jj@example.com","telephoneNumber":"124567890","identifyVerificationId":"8e37092e-6b95-46fc-82ee-2308d5360c51","effectiveFrom":"2017-04-03"}},
      |	{"personLatestDetail":{"id":1461440825,"addressUnitId":-1448217472,"firstName":"Jim","lastName":"James","emailAddress":"jj@example.com","telephoneNumber":"458146","identifyVerificationId":"163f8db8-fc8d-4056-be45-3cb8579a805b","effectiveFrom":"2017-04-04"}}
      |]}""".stripMargin

  private val inputOrgJsonNoPhone =
    """{
      |"id":1000000003,
      |"governmentGatewayGroupId":"stub-group-3",
      |"representativeCode":990551132,
      |"organisationLatestDetail":
      |{
      |	"addressUnitId":1000000000,
      |	"representativeFlag":false,
      |	"smallBusinessFlag":true,
      |	"organisationName":"Automated Stub 3",
      |	"organisationEmailAddress":"stub3@voa.gov.uk",
      |	"effectiveFrom":"2017-02-16"
      |},
      |"persons":[
      |	{"personLatestDetail":{"id":846189244,"addressUnitId":-1815520542,"firstName":"Jim","lastName":"James","emailAddress":"jj@example.com","telephoneNumber":"124567890","identifyVerificationId":"ecd6d989-05a5-4c06-8528-055d46ab5ce6","effectiveFrom":"2017-03-30"}},
      |	{"personLatestDetail":{"id":619144174,"addressUnitId":-1674062666,"firstName":"Jim","lastName":"James","emailAddress":"jj@example.com","telephoneNumber":"124567890","identifyVerificationId":"d1e765fa-f208-4530-b634-fe366ec5984b","effectiveFrom":"2017-03-31"}},
      |	{"personLatestDetail":{"id":1802901214,"addressUnitId":1160458860,"firstName":"Jim","lastName":"James","emailAddress":"jj@example.com","telephoneNumber":"124567890","identifyVerificationId":"8e37092e-6b95-46fc-82ee-2308d5360c51","effectiveFrom":"2017-04-03"}},
      |	{"personLatestDetail":{"id":1461440825,"addressUnitId":-1448217472,"firstName":"Jim","lastName":"James","emailAddress":"jj@example.com","telephoneNumber":"458146","identifyVerificationId":"163f8db8-fc8d-4056-be45-3cb8579a805b","effectiveFrom":"2017-04-04"}}
      |]}""".stripMargin

  private val outputOrgJson =
    """{
      |"id":1000000003,
      |"groupId":"stub-group-3",
      |"companyName":"Automated Stub 3",
      |"addressId":1000000000,
      |"email":"stub3@voa.gov.uk",
      |"phone":"0123456783",
      |"isSmallBusiness":true,
      |"isAgent":false,
      |"agentCode":990551132
      |}""".stripMargin.replaceAll("\n", "")

  private val inputPersonJson =
    """{
      |"id":956459863,
      |"governmentGatewayExternalId":"Ext-cbdabe06-a2b7-46c6-8546-39fe9fe1c27d",
      |"personLatestDetail":
      |{
      | "id":956459863,
      |	"addressUnitId":1000000000,
      |	"firstName":"Jim",
      |	"lastName":"James",
      |	"emailAddress":"j@example.com",
      |	"telephoneNumber":"0123456789",
      |	"mobileNumber":"0123456789",
      |	"identifyVerificationId":"9f87e199-c4b8-461e-9498-b4dafc98d4be",
      |	"effectiveFrom":"2017-03-20"
      |},
      |"organisationId":1000000003,
      |"organisationLatestDetail":
      |{
      |	"addressUnitId":1000000000,
      |	"representativeFlag":false,
      |	"smallBusinessFlag":true,
      |	"organisationName":"Automated Stub 3",
      |	"organisationEmailAddress":"stub3@voa.gov.uk",
      |	"organisationTelephoneNumber":"0123456783",
      |	"effectiveFrom":"2017-02-16"
      |}
      |}""".stripMargin

  private val inputPersonJsonNoPhone =
    """{
      |"id":956459863,
      |"governmentGatewayExternalId":"Ext-cbdabe06-a2b7-46c6-8546-39fe9fe1c27d",
      |"personLatestDetail":
      |{
      | "id":956459863,
      |	"addressUnitId":1000000000,
      |	"firstName":"Jim",
      |	"lastName":"James",
      |	"emailAddress":"j@example.com",
      |	"identifyVerificationId":"9f87e199-c4b8-461e-9498-b4dafc98d4be",
      |	"effectiveFrom":"2017-03-20"
      |},
      |"organisationId":1000000003,
      |"organisationLatestDetail":
      |{
      |	"addressUnitId":1000000000,
      |	"representativeFlag":false,
      |	"smallBusinessFlag":true,
      |	"organisationName":"Automated Stub 3",
      |	"organisationEmailAddress":"stub3@voa.gov.uk",
      |	"organisationTelephoneNumber":"0123456783",
      |	"effectiveFrom":"2017-02-16"
      |}
      |}""".stripMargin

  private val outputPersonJson =
    """{
     |"externalId":"Ext-cbdabe06-a2b7-46c6-8546-39fe9fe1c27d",
     |"trustId":"9f87e199-c4b8-461e-9498-b4dafc98d4be",
     |"organisationId":1000000003,
     |"individualId":956459863,
     |"details":{
     |"firstName":"Jim",
     |"lastName":"James",
     |"email":"j@example.com",
     |"phone1":"0123456789",
     |"phone2":"0123456789",
     |"addressId":1000000000
     |}
     |}""".stripMargin.replaceAll("\n", "")

  private val validOrg = Organisation(id = 1000000003, groupId = "stub-group-3", companyName = "Automated Stub 3",
    addressId = 1000000000, email = "stub3@voa.gov.uk", phone = "0123456783",
    isSmallBusiness = true, isAgent = false, agentCode = 990551132)

  private val validOrgNoPhone = validOrg.copy(phone = "not set")

  private val validPerson = Person(externalId = "Ext-cbdabe06-a2b7-46c6-8546-39fe9fe1c27d",
    trustId = "9f87e199-c4b8-461e-9498-b4dafc98d4be", organisationId = 1000000003, individualId = 956459863,
    details = PersonDetails(firstName = "Jim", lastName = "James", email = "j@example.com", phone1 = "0123456789",
    phone2 = Some("0123456789"), addressId = 1000000000))

  private val validPersonNoPhone = validPerson.copy(details = validPerson.details.copy(phone1 = "not set", phone2 = None))

  private val mockServicesConfig = mock[ServicesConfig]
  private val mockWsHttp = mock[WSHttp]
  private val connector = new BackendConnector(mockWsHttp, mockServicesConfig)

  implicit val hc = HeaderCarrier()

  when(mockWsHttp.GET[Option[Organisation]](contains("?governmentGatewayGroupId=NOT_FOUND"))(any[HttpReads[Option[Organisation]]], refEq(hc)))
    .thenReturn(Future.successful(None))

  when(mockWsHttp.GET[Option[Organisation]](contains("?governmentGatewayGroupId=stub-group-3"))(any[HttpReads[Option[Organisation]]], refEq(hc)))
    .thenReturn(Future.successful(Some(validOrg)))

  when(mockWsHttp.GET[Option[Person]](contains("?governmentGatewayExternalId=NO_PERSON"))(any[HttpReads[Option[Person]]], refEq(hc)))
    .thenReturn(Future.successful(None))

  when(mockWsHttp.GET[Option[Person]](contains("?governmentGatewayExternalId=extId"))(any[HttpReads[Option[Person]]], refEq(hc)))
    .thenReturn(Future.successful(Some(validPerson)))


  "Json parsing from backend structures [Reads]" should {
    "Correctly parse an Organisation" in {
      Json.parse(inputOrgJson).validate[Organisation] mustBe JsSuccess(validOrg)
    }

    "Correctly parse an Organisation with no phone" in {
      Json.parse(inputOrgJsonNoPhone).validate[Organisation] mustBe JsSuccess(validOrgNoPhone)
    }

    "Correctly parse a Person" in {
      Json.parse(inputPersonJson).validate[Person] mustBe JsSuccess(validPerson)
    }

    "Correctly parse a Person with no phone" in {
      Json.parse(inputPersonJsonNoPhone).validate[Person] mustBe JsSuccess(validPersonNoPhone)
    }
  }

  "Json translation to internal structures [Writes]" should {
    "Correctly render an Organisation" in {
      Json.toJson(validOrg).toString() mustBe outputOrgJson
    }

    "Correctly render a Person" in {
      Json.toJson(validPerson).toString() mustBe outputPersonJson
    }
  }

  "The connector when translating a GET" should {
    "for a not found Organisation return a 'None'" in {
      await(connector.getOrganisation("NOT_FOUND")) mustBe None
    }

    "for a found Organisation return a 'Some(Organisation)'" in {
      await(connector.getOrganisation("stub-group-3")) mustBe Some(validOrg)
    }

    "for a not found Person return a 'None'" in {
      await(connector.getPerson("NO_PERSON")) mustBe None
    }

    "for a found Person return a 'Some(Person)'" in {
      await(connector.getPerson("extId")) mustBe Some(validPerson)
    }
  }
}
