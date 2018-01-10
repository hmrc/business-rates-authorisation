/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import businessrates.authorisation.models.Accounts
import com.google.inject.ImplementedBy
import play.api.libs.json.Json
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDateTime, BSONDocument}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AccountsMongoCache])
trait AccountsCache {
  def cache(sessionId: String, accounts: Accounts): Future[Unit]
  def get(sessionId: String): Future[Option[Accounts]]
  def drop(sessionId: String): Future[Unit]
}

class AccountsMongoCache @Inject()(db: DB)(implicit ec: ExecutionContext) extends ReactiveRepository[Record, String]("accountsCache", () => db, Record.mongoFormat, implicitly) with AccountsCache {

  override def indexes: Seq[Index] = Seq(Index(key = Seq("createdAt" -> IndexType.Ascending), name = Some("ttl"), options = BSONDocument("expireAfterSeconds" -> 900)))

  override def cache(sessionId: String, accounts: Accounts): Future[Unit] = {
    insert(Record(sessionId, accounts)) map { _ => () }
  }

  override def get(sessionId: String): Future[Option[Accounts]] = {
    findById(sessionId) map { _.map(_.data) }
  }

  override def drop(sessionId: String): Future[Unit] = {
    removeById(sessionId) map { _ => () }
  }
}

private[repositories] case class Record(_id: String, data: Accounts, createdAt: BSONDateTime = BSONDateTime(System.currentTimeMillis))

private[repositories] object Record {
  private implicit val dateFormat = reactivemongo.json.BSONFormats.BSONDateTimeFormat

  val mongoFormat = Json.format[Record]
}
