/*
 * Copyright 2023 HM Revenue & Customs
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

package businessrates.authorisation.services.jobHandler

import akka.actor.ActorSystem
import play.api.{Configuration, Logger, LoggerLike}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

@Singleton
class AccountsCacheScheduler @Inject()(
      configuration: Configuration,
      accountsCacheJobHandler: AccountsCacheJobHandler,
      actorSystem: ActorSystem)(implicit executionContext: ExecutionContext) {

  val log: LoggerLike = Logger(this.getClass)
  private lazy val housekeepingIntervalMinutes: Int = configuration.get[Int]("housekeepingIntervalMinutes")

  actorSystem.scheduler.scheduleWithFixedDelay(initialDelay = 0 seconds, delay = housekeepingIntervalMinutes minutes) {
    () =>
      accountsCacheJobHandler.processJob()
  }
}
