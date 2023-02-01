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

import businessrates.authorisation.models.{Accounts, MongoLocalDateTimeFormat}
import com.google.inject.ImplementedBy
import org.bson.BsonType
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.model.{IndexModel, IndexOptions, Projections}
import play.api.libs.json.{Json, OFormat, Reads, __}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.SECONDS
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AccountsMongoCache])
trait AccountsCache {
  def cache(sessionId: String, accounts: Accounts): Future[Unit]

  def get(sessionId: String): Future[Option[Accounts]]

  def drop(sessionId: String): Future[Unit]

  def updateCreatedAtTimestampById(ids: Seq[String]): Future[Long]

  def getRecordsWithIncorrectTimestamp: Future[Seq[String]]
}

@Singleton
class AccountsMongoCache @Inject()(db: MongoComponent)(implicit ec: ExecutionContext)
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
  override def updateCreatedAtTimestampById(ids: Seq[String]): Future[Long] =
    collection
      .updateMany(filter = in("_id", ids: _*), update = set("createdAt", LocalDateTime.now()))
      .toFuture()
      .map(_.getModifiedCount)
  override def getRecordsWithIncorrectTimestamp: Future[Seq[String]] = {
    val longReads: Reads[String] = (__ \ "_id").read[String]
    collection
      .find[BsonValue](not(`type`("createdAt", BsonType.DATE_TIME)))
      .projection(Projections.exclude("createdAt", "data"))
      .limit(10000)
      .toFuture()
      .map(_.map(bson => Codecs.fromBson[String](bson)(longReads)))
  }
}

case class Record(_id: String, data: Accounts, createdAt: LocalDateTime = LocalDateTime.now())

object Record {
  implicit val dateFormat = MongoLocalDateTimeFormat.localDateTimeFormat
  implicit val mongoFormats: OFormat[Record] = Json.format[Record]
}
