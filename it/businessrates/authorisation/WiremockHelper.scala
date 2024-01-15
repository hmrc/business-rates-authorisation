package businessrates.authorisation

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.concurrent.{Eventually, IntegrationPatience}

object WiremockHelper extends Eventually with IntegrationPatience {

  val wiremockPort: Int = 11111
  val wiremockHost: String = "localhost"

  def verifyPost(uri: String, optBody: Option[String] = None): Unit = {
    val uriMapping = postRequestedFor(urlEqualTo(uri))
    val postRequest = optBody match {
      case Some(body) => uriMapping.withRequestBody(equalTo(body))
      case None       => uriMapping
    }
    verify(postRequest)
  }

  def verifyGet(uri: String): Unit =
    verify(getRequestedFor(urlEqualTo(uri)))

  def stubGet(url: String, status: Integer, body: String): Unit =
    stubFor(
      get(urlMatching(url))
        .willReturn(
          aResponse().withStatus(status).withBody(body)
        ))

  def stubPost(url: String, status: Integer, responseBody: String): Unit =
    stubFor(
      post(urlMatching(url))
        .willReturn(
          aResponse().withStatus(status).withBody(responseBody)
        ))

  def stubPut(url: String, status: Integer, responseBody: String): Unit =
    stubFor(
      put(urlMatching(url))
        .willReturn(
          aResponse().withStatus(status).withBody(responseBody)
        ))

  def stubPatch(url: String, status: Integer, responseBody: String): Unit =
    stubFor(
      patch(urlMatching(url))
        .willReturn(
          aResponse().withStatus(status).withBody(responseBody)
        ))

  def stubDelete(url: String, status: Integer, responseBody: String): Unit =
    stubFor(
      delete(urlMatching(url))
        .willReturn(
          aResponse().withStatus(status).withBody(responseBody)
        ))
}

trait WiremockHelper {

  import WiremockHelper._

  lazy val wmConfig: WireMockConfiguration = wireMockConfig().port(wiremockPort)
  lazy val wireMockServer: WireMockServer = new WireMockServer(wmConfig)

  def startWiremock(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  def stopWiremock(): Unit = wireMockServer.stop()

  def resetWiremock(): Unit = WireMock.reset()
}
