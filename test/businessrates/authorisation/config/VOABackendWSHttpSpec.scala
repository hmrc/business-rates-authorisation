/*
 * Copyright 2022 HM Revenue & Customs
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

import akka.actor.ActorSystem
import businessrates.authorisation.connectors.VOABackendWSHttp
import businessrates.authorisation.utils.{TestConfiguration, WireMockSpec}
import com.codahale.metrics.{Counter, Meter, MetricRegistry, Timer}
import com.kenshoo.play.metrics.Metrics
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import akka.util.Timeout
import org.scalatest.time.{Millis, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.ws
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.await
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.ws.WSRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VOABackendWSHttpSpec extends AnyWordSpec with WireMockSpec with MockitoSugar with TestConfiguration {

  implicit val timeout: Timeout = Timeout(Span(250, Millis))

  val metricsMock = mock[Metrics]
  val metricRegistry = mock[MetricRegistry]
  val mockTimer = mock[Timer]
  val mockWSClient = mock[WSClient]

  when(metricsMock.defaultRegistry).thenReturn(metricRegistry)

  when(metricRegistry.timer(any[String])).thenReturn(mockTimer)
  when(metricRegistry.counter(any[String])).thenReturn(mock[Counter])
  when(metricRegistry.meter(any[String])).thenReturn(mock[Meter])

  when(mockTimer.time()).thenReturn(mock[Timer.Context])

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val backendWSHttp =
    new VOABackendWSHttp(configuration, metricsMock, mock[AuditConnector], mockWSClient, mock[ActorSystem])
    with MockWsRequest {
      val status = 200
    }
  val failHttp =
    new VOABackendWSHttp(configuration, metricsMock, mock[AuditConnector], mock[WSClient], mock[ActorSystem])
    with MockWsRequest {
      val status = 400
    }

  "Invoking a method on the VOABackendWSHttp connection" should {
    "trigger metrics success logging for the customer-management-api endpoint" in {
      val url = "http://voa-api-proxy.service:80/customer-management-api/organisation"

      await(backendWSHttp.doGet(url, headers = Seq.empty))

      verify(metricRegistry, times(1)).counter("customer-management-api/success-counter")
      verify(metricRegistry, times(1)).meter("customer-management-api/success-meter")
    }

    "trigger metrics success logging for the address-management-api endpoint with JSON query" in {
      val url =
        "http://voa-api-proxy.service:80/address-management-api/address?pageSize=100&startPoint=1&SearchParameters={\"postcode\": \"BN12 6EA\"}"

      await(backendWSHttp.doGet(url, headers = Seq.empty))

      verify(metricRegistry, times(1)).counter("address-management-api/success-counter")
      verify(metricRegistry, times(1)).meter("address-management-api/success-meter")
    }

    "trigger metrics failure logging for the address-management-api endpoint with JSON query" in {
      val url =
        "http://voa-api-proxy.service:80/address-management-api/address?pageSize=100&startPoint=1&SearchParameters={\"postcode\": \"BN12 6EA\"}"

      await(failHttp.doGet(url, headers = Seq.empty))

      verify(metricRegistry, times(1)).counter("address-management-api/failed-counter")
      verify(metricRegistry, times(1)).meter("address-management-api/failed-meter")
    }
  }

  trait MockWsRequest extends WSRequest {
    val status: Int

    override def buildRequest[A](url: String, headers: Seq[(String, String)]): ws.WSRequest = {
      val mockRequest = mock[play.api.libs.ws.WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockRequest.withMethod(any())).thenReturn(mockRequest)
      when(mockRequest.execute()).thenReturn(Future.successful(mockResponse))
      when(mockResponse.status).thenReturn(status)

      mockRequest
    }

  }
}
