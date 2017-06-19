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

import businessrates.authorisation.connectors.{BackendConnector, OrganisationAccounts, PersonAccounts, PropertyLinking}
import com.google.inject.{AbstractModule, Inject, Provider}
import com.google.inject.name.Names
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import uk.gov.hmrc.play.audit.filters.AuditFilter
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.auth.microservice.filters.AuthorisationFilter
import uk.gov.hmrc.play.config.inject.{DefaultServicesConfig, ServicesConfig}
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.http.logging.filters.LoggingFilter
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal


class GuiceModule(override val environment: Environment, configuration: Configuration) extends AbstractModule with ServicesConfig {
  override val runModeConfiguration = configuration

  def configure(): Unit = {
    bindConstant().annotatedWith(Names.named("voaApiSubscriptionHeader")).to(configuration.getString("voaApi.subscriptionKeyHeader").get)
    bindConstant().annotatedWith(Names.named("voaApiTraceHeader")).to(configuration.getString("voaApi.subscriptionKeyHeader").get)
    bindConstant().annotatedWith(Names.named("dataPlatformUrl")).to(baseUrl("data-platform"))
    bindConstant().annotatedWith(Names.named("ratesListYear")).to(getConfInt("rates.list.year", 2017))
    bind(classOf[WSHttp]).annotatedWith(Names.named("voaBackendWSHttp")).to(classOf[VOABackendWSHttp])
    bind(classOf[WSHttp]).annotatedWith(Names.named("simpleWSHttp")).to(classOf[SimpleWSHttp])
    bind(classOf[ServicesConfig]).to(classOf[DefaultServicesConfig])
    bind(classOf[OrganisationAccounts]).to(classOf[BackendConnector])
    bind(classOf[PersonAccounts]).to(classOf[BackendConnector])
    bind(classOf[PropertyLinking]).to(classOf[BackendConnector])
    bind(classOf[DB]).toProvider(classOf[MongoProvider]).asEagerSingleton()
  }
}

class MongoProvider @Inject()(reactiveMongoComponent: ReactiveMongoComponent) extends Provider[DB] {
  override def get(): DB = reactiveMongoComponent.mongoConnector.db()
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs: Config = Play.current.configuration.underlying.as[Config]("controllers")
}

object AuthParamsControllerConfiguration extends AuthParamsControllerConfig {
  lazy val controllerConfigs: Config = ControllerConfiguration.controllerConfigs
}

object MicroserviceAuditFilter extends AuditFilter with AppName with MicroserviceFilterSupport {
  override val auditConnector = MicroserviceAuditConnector
  override def controllerNeedsAuditing(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

object MicroserviceLoggingFilter extends LoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MicroserviceAuthFilter extends AuthorisationFilter with MicroserviceFilterSupport {
  override lazy val authParamsConfig = AuthParamsControllerConfiguration
  override lazy val authConnector = MicroserviceAuthConnector

  override def controllerNeedsAuth(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsAuth
}

object MicroserviceGlobal extends DefaultMicroserviceGlobal with RunMode with MicroserviceFilterSupport {
  override val auditConnector = MicroserviceAuditConnector
  override val loggingFilter = MicroserviceLoggingFilter
  override val microserviceAuditFilter = MicroserviceAuditFilter
  override val authFilter = Some(MicroserviceAuthFilter)

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")
}
