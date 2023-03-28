package businessrates.authorisation

import businessrates.authorisation.WiremockHelper.{wiremockHost, wiremockPort}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.DefaultAwaitTimeout

trait BaseIntegrationSpec
  extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with DefaultAwaitTimeout
    with WiremockHelper {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build()

  def config: Map[String, String] = Map(
    "auditing.enabled" -> "false",
    "microservice.services.data-platform.host" -> wiremockHost,
    "microservice.services.data-platform.port" -> wiremockPort.toString,
    "microservice.services.voa-bst.host" -> wiremockHost,
    "microservice.services.voa-bst.port" -> wiremockPort.toString
  )

  override def beforeAll(): Unit = {
    startWiremock()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    resetWiremock()
    super.beforeEach()
  }
}
