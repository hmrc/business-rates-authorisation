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
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.ws
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSRequest
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class VOABackendWSHttpSpec extends UnitSpec with WireMockSpec with MockitoSugar {

  val metricsMock = mock[Metrics]
  val metricRegistry = mock[MetricRegistry]
  val mockTimer = mock[Timer]

  when(metricsMock.defaultRegistry).thenReturn(metricRegistry)

  when(metricRegistry.timer(any[String])).thenReturn(mockTimer)
  when(metricRegistry.counter(any[String])).thenReturn(mock[Counter])
  when(metricRegistry.meter(any[String])).thenReturn(mock[Meter])

  when(mockTimer.time()).thenReturn(mock[Timer.Context])

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val wsHttp = new VOABackendWSHttp(metricsMock) with MockWsRequest { val status = 200 }
  val failHttp = new VOABackendWSHttp(metricsMock) with MockWsRequest { val status = 400 }

  "Invoking a method on the VOABackendWSHttp connection" should {
    "trigger metrics success logging for the customer-management-api endpoint" in {
      val url = "http://voa-api-proxy.service:80/customer-management-api/organisation"

      await(wsHttp.doGet(url))

      verify(metricRegistry, times(1)).counter("customer-management-api/success-counter")
      verify(metricRegistry, times(1)).meter("customer-management-api/success-meter")
    }

    "trigger metrics success logging for the mdtp-dashboard-management-api endpoint - irrespective of query params" in {
      val url = "http://voa-api-proxy.service:80/mdtp-dashboard-management-api/mdtp_dashboard/properties_view?listYear=2016&organisationId=101"

      await(wsHttp.doGet(url))

      verify(metricRegistry, times(1)).counter("mdtp-dashboard-management-api/success-counter")
      verify(metricRegistry, times(1)).meter("mdtp-dashboard-management-api/success-meter")
    }

    "trigger metrics success logging for the address-management-api endpoint with JSON query" in {
      val url = "http://voa-api-proxy.service:80/address-management-api/address?pageSize=100&startPoint=1&SearchParameters={\"postcode\": \"BN12 6EA\"}"

      await(wsHttp.doGet(url))

      verify(metricRegistry, times(1)).counter("address-management-api/success-counter")
      verify(metricRegistry, times(1)).meter("address-management-api/success-meter")
    }

    "trigger metrics failure logging for the address-management-api endpoint with JSON query" in {
      val url = "http://voa-api-proxy.service:80/address-management-api/address?pageSize=100&startPoint=1&SearchParameters={\"postcode\": \"BN12 6EA\"}"

      await(failHttp.doGet(url))

      verify(metricRegistry, times(1)).counter("address-management-api/failed-counter")
      verify(metricRegistry, times(1)).meter("address-management-api/failed-meter")
    }
  }

  trait MockWsRequest extends WSRequest {
    val status: Int

    override def buildRequest[A](url: String)(implicit hc: HeaderCarrier): ws.WSRequest = {
      val mockRequest = mock[play.api.libs.ws.WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockRequest.get()).thenReturn(Future.successful(mockResponse))
      when(mockResponse.status).thenReturn(status)

      mockRequest
    }
  }
}
