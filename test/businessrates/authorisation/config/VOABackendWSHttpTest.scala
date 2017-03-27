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

package businessrates.authorisation.config

import businessrates.authorisation.utils.WireMockSpec
import com.codahale.metrics.{Counter, Meter, MetricRegistry, Timer}
import com.kenshoo.play.metrics.Metrics
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

class VOABackendWSHttpTest extends UnitSpec with WireMockSpec with MockitoSugar {

  val metricsMock = mock[Metrics]
  val metricRegistry = mock[MetricRegistry]

  when(metricsMock.defaultRegistry).thenReturn(metricRegistry)

  when(metricRegistry.timer(any[String])).thenReturn(mock[Timer])
  when(metricRegistry.counter(any[String])).thenReturn(mock[Counter])
  when(metricRegistry.meter(any[String])).thenReturn(mock[Meter])

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val wsHttp = new VOABackendWSHttp(metricsMock, "subKey", "true")

  "when extracting the API name to use for the request metrics" should {
    "the API name should be extracted from a simple URL" in {
      val url = "http://voa-api-proxy.service:80/customer-management-api/organisation"

      wsHttp.getApiName(url) shouldBe "customer-management-api"
    }

    "the API name should be extracted from a URL with query params" in {
      val url = "http://voa-api-proxy.service:80/mdtp-dashboard-management-api/mdtp_dashboard/properties_view?listYear=2016&organisationId=101"

      wsHttp.getApiName(url) shouldBe "mdtp-dashboard-management-api"
    }

    "the API name should be extracted from a URL with query params including JSON" in {
      val url = "http://voa-api-proxy.service:80/address-management-api/address?pageSize=100&startPoint=1&SearchParameters={\"postcode\": \"BN12 6EA\"}"

      wsHttp.getApiName(url) shouldBe "address-management-api"
    }
  }
}
