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

import businessrates.authorisation.connectors.{BackendConnector, OrganisationAccounts, PersonAccounts}
import businessrates.authorisation.controllers.{VoaIds, WithIds}
import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}

// keep the two unused parameters, as that's the constructor Guice expects to find
class GuiceModule(
      environment: Environment,
      configuration: Configuration
) extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[OrganisationAccounts]).to(classOf[BackendConnector])
    bind(classOf[PersonAccounts]).to(classOf[BackendConnector])
    bind(classOf[WithIds]).to(classOf[VoaIds])
  }
}
