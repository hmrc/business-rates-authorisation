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

package businessrates.authorisation.repositories

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import businessrates.authorisation.models.Accounts
import com.google.inject.ImplementedBy
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.model.Filters._
import scala.concurrent.duration.SECONDS
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.localDateTimeFormat

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AccountsMongoCache])
trait AccountsCache {
  def cache(sessionId: String, accounts: Accounts): Future[Unit]
  def get(sessionId: String): Future[Option[Accounts]]
  def drop(sessionId: String): Future[Unit]
}

@Singleton
class AccountsMongoCache @Inject()(db: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Record](
      mongoComponent = db,
      collectionName = "accountsCache",
      domainFormat = Record.mongoFormat,
      indexes = Seq(
        IndexModel(
          ascending("createdAt"),
          IndexOptions()
            .name("ttl")
            .unique(false)
            .expireAfter(900L, SECONDS)))
    ) with AccountsCache {

  override def cache(sessionId: String, accounts: Accounts): Future[Unit] =
    collection.insertOne(Record(sessionId, accounts)).toFuture().map { _ =>
      ()
    }

  override def get(sessionId: String): Future[Option[Accounts]] =
    collection.find(equal("_id", sessionId)).map(_.data).toSingle().toFutureOption()

  override def drop(sessionId: String): Future[Unit] =
    collection.findOneAndDelete(equal("_id", sessionId)).toFuture().map { _ =>
      ()
    }

}

private[repositories] case class Record(_id: String, data: Accounts, createdAt: LocalDateTime = LocalDateTime.now())

private[repositories] object Record {

  private implicit val dateFormat = localDateTimeFormat
  val mongoFormat = Json.format[Record]
}
