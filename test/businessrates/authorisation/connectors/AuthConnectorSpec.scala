/*
 * Copyright 2019 HM Revenue & Customs
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

import businessrates.authorisation.ArbitraryDataGeneration
import businessrates.authorisation.config.WSHttp
import businessrates.authorisation.models.{Authority, UserDetails}
import org.mockito.ArgumentMatchers.{any => anyT, eq => isEqual, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class AuthConnectorSpec extends WordSpec with MustMatchers with MockitoSugar with BeforeAndAfterEach
  with FutureAwaits with DefaultAwaitTimeout with ArbitraryDataGeneration {

  implicit val hc = HeaderCarrier()

  private val mockWsHttp = mock[WSHttp]
  private val mockServicesConfig = mock[ServicesConfig]
  private val connector = new AuthConnector(mockWsHttp, mockServicesConfig)

  private val authUrl = "http://localhost:99999999"

  "AuthConnector " should {

    "return 200 OK when GovernmentGatewayDetails are valid" in {
      when(mockServicesConfig.baseUrl(isEqual("auth"))).thenReturn(authUrl)

      val userDetails = UserDetails(Some("group-identifier-1"), Some("Organisation"))
      val authority = Authority("/authorityUri", Some("/userDetailsLink"), Some("/idsUri"))
      val ids: JsValue = Json.parse("""{"externalId":"external-id-1"}""")

      when(mockWsHttp.GET[Option[JsValue]](contains(s"$authUrl/idsUri"))(anyT[HttpReads[Option[JsValue]]], anyT[HeaderCarrier], anyT[ExecutionContext]))
        .thenReturn(Future.successful(Some(ids)))

      when(mockWsHttp.GET[Option[UserDetails]](contains("/userDetailsLink"))(anyT[HttpReads[Option[UserDetails]]], anyT[HeaderCarrier], anyT[ExecutionContext]))
        .thenReturn(Future.successful(Some(userDetails)))

      when(mockWsHttp.GET[Option[Authority]](contains(s"$authUrl/auth/authority"))(anyT[HttpReads[Option[Authority]]], anyT[HeaderCarrier], anyT[ExecutionContext]))
        .thenReturn(Future.successful(Some(authority)))

      val governmentGatewayDetails = await(connector.getGovernmentGatewayDetails)

      governmentGatewayDetails.isDefined mustBe true
      governmentGatewayDetails.get.affinityGroup mustBe Some("Organisation")
      governmentGatewayDetails.get.externalId mustBe "external-id-1"
      governmentGatewayDetails.get.groupId mustBe Some("group-identifier-1")

    }

  }


}
