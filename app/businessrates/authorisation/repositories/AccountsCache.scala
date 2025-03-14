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

package businessrates.authorisation.repositories

import businessrates.authorisation.models.{Accounts, MongoLocalDateTimeFormat}
import com.google.inject.ImplementedBy
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, ReplaceOptions}
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.SECONDS
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AccountsMongoCache])
trait AccountsCache {
  def cache(sessionId: String, accounts: Accounts): Future[Unit]

  def get(sessionId: String): Future[Option[Accounts]]

  def drop(sessionId: String): Future[Unit]

  def getAll: Future[Seq[Record]]
}

@Singleton
class AccountsMongoCache @Inject() (db: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Record](
      mongoComponent = db,
      collectionName = "accountsCache",
      domainFormat = Record.mongoFormats,
      indexes = Seq(
        IndexModel(
          ascending("createdAt"),
          IndexOptions()
            .name("ttl")
            .unique(false)
            .expireAfter(900L, SECONDS)
        )
      )
    ) with AccountsCache {

  override def cache(sessionId: String, accounts: Accounts): Future[Unit] =
    collection
      .replaceOne(
        filter = Filters.eq("_id", sessionId),
        replacement = Record(sessionId, accounts),
        options = new ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())

  override def get(sessionId: String): Future[Option[Accounts]] =
    collection.find(equal("_id", sessionId)).map(_.data).toSingle().toFutureOption()

  override def drop(sessionId: String): Future[Unit] =
    collection.findOneAndDelete(equal("_id", sessionId)).toFuture().map { _ =>
      ()
    }

  // Implemented for IT test purpose only
  def getAll: Future[Seq[Record]] =
    collection.find().toFuture()
}

case class Record(_id: String, data: Accounts, createdAt: LocalDateTime = LocalDateTime.now())

object Record {
  implicit val dateFormat: Format[LocalDateTime] = MongoLocalDateTimeFormat.localDateTimeFormat
  implicit val mongoFormats: OFormat[Record] = Json.format[Record]
}
