/*
 * Copyright 2024 HM Revenue & Customs
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

package businessrates.authorisation

import businessrates.authorisation.WiremockHelper.{wiremockHost, wiremockPort}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.DefaultAwaitTimeout

trait BaseIntegrationSpec
    extends AnyWordSpec with Matchers with GuiceOneServerPerSuite with BeforeAndAfterEach with BeforeAndAfterAll
    with DefaultAwaitTimeout with WiremockHelper {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build()

  lazy val ws = app.injector.instanceOf[WSClient]

  lazy val baseUrl: String = s"http://localhost:$port/business-rates-authorisation"

  def config: Map[String, String] =
    Map(
      "auditing.enabled"                         -> "false",
      "microservice.services.data-platform.host" -> wiremockHost,
      "microservice.services.data-platform.port" -> wiremockPort.toString,
      "microservice.services.voa-bst.host"       -> wiremockHost,
      "microservice.services.voa-bst.port"       -> wiremockPort.toString,
      "microservice.services.auth.host"          -> wiremockHost,
      "microservice.services.auth.port"          -> wiremockPort.toString
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
