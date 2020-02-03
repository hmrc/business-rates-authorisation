/*
 * Copyright 2020 HM Revenue & Customs
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

import businessrates.authorisation.auth.DefaultAuthConnector
import businessrates.authorisation.connectors.{BackendConnector, OrganisationAccounts, PersonAccounts, PropertyLinking}
import businessrates.authorisation.controllers.{VoaIds, WithIds}
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Inject, Provider}
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.Mode.Mode
import play.api.{Application, Configuration, Environment, Play}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpPost
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode, ServicesConfig}
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal
import uk.gov.hmrc.play.microservice.filters.{AuditFilter, LoggingFilter, MicroserviceFilterSupport}

class GuiceModule(
      environment: Environment,
      configuration: Configuration
) extends AbstractModule {

  private val servicesConfig = new ServicesConfig {
    override protected def mode: Mode = environment.mode

    override protected def runModeConfiguration: Configuration = configuration

  }
  def configure(): Unit = {
    bindConstant().annotatedWith(Names.named("dataPlatformUrl")).to(servicesConfig.baseUrl("data-platform"))
    bindConstant().annotatedWith(Names.named("ratesListYear")).to(servicesConfig.getConfInt("rates.list.year", 2017))
    bindConstant().annotatedWith(Names.named("authBaseUrl")).to(servicesConfig.getConfInt("auth.baseUrl", 2017))
    bind(classOf[WSHttp]).annotatedWith(Names.named("voaBackendWSHttp")).to(classOf[VOABackendWSHttp])
    bind(classOf[WSHttp]).annotatedWith(Names.named("simpleWSHttp")).to(classOf[SimpleWSHttp])
    bind(classOf[ServicesConfig]).toInstance(servicesConfig)
    bind(classOf[OrganisationAccounts]).to(classOf[BackendConnector])
    bind(classOf[PersonAccounts]).to(classOf[BackendConnector])
    bind(classOf[PropertyLinking]).to(classOf[BackendConnector])
    bind(classOf[WithIds]).to(classOf[VoaIds])
    bind(classOf[DB]).toProvider(classOf[MongoProvider]).asEagerSingleton()
    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector])
    bind(classOf[HttpPost]).to(classOf[SimpleWSHttp])
  }
}

class MongoProvider @Inject()(reactiveMongoComponent: ReactiveMongoComponent) extends Provider[DB] {
  override def get(): DB = reactiveMongoComponent.mongoConnector.db()
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs: Config = Play.current.configuration.underlying.as[Config]("controllers")
}

object MicroserviceAuditFilter extends AuditFilter with AppName with MicroserviceFilterSupport {
  override val auditConnector = MicroserviceAuditConnector

  override def controllerNeedsAuditing(controllerName: String): Boolean =
    ControllerConfiguration.paramsForController(controllerName).needsAuditing

  override protected def appNameConfiguration: Configuration = Play.current.configuration
}

object MicroserviceLoggingFilter extends LoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String): Boolean =
    ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MicroserviceGlobal extends DefaultMicroserviceGlobal with RunMode with MicroserviceFilterSupport {
  override val auditConnector = MicroserviceAuditConnector
  override val loggingFilter = MicroserviceLoggingFilter
  override val microserviceAuditFilter = MicroserviceAuditFilter
  override val authFilter = None

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] =
    app.configuration.getConfig(s"microservice.metrics")

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

private case class ConfigMissing(key: String) extends Exception(s"Missing config for $key")
