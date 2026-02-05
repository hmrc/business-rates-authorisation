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

package businessrates.authorisation.connectors

import businessrates.authorisation.models._
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ModernisedBackendConnector @Inject() (val http: HttpClientV2, servicesConfig: ServicesConfig)
    extends BackendConnector {

  lazy val backendUrl: String = servicesConfig.baseUrl("data-platform")

  val groupAccountsUrl = s"$backendUrl/customer-management-api/organisation"
  val individualAccountsUrl: String = s"$backendUrl/customer-management-api/person"

  override def getOrganisationByGGId(
        ggId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]] =
    getOrganisation(ggId, "governmentGatewayGroupId")

  override def getPerson(
        externalId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Person]] = {
    implicit val apiFormat: Reads[Person] = Person.apiFormat
    val url = s"$individualAccountsUrl?governmentGatewayExternalId=$externalId"
    http
      .get(url"$url")
      .withProxy
      .execute[Option[Person]]
  }

  private def getOrganisation(id: String, paramName: String)(implicit
        hc: HeaderCarrier,
        ec: ExecutionContext
  ): Future[Option[Organisation]] = {
    implicit val apiFormat: Reads[Organisation] = Organisation.apiFormat
    val url = s"$groupAccountsUrl?$paramName=$id"
    http
      .get(url"$url")
      .withProxy
      .execute[Option[Organisation]]
  }

  override def updateCredentials(personId: String, groupId: String, externalId: String)(implicit
        hc: HeaderCarrier,
        ec: ExecutionContext
  ): Future[Unit] = {
    val url = s"$backendUrl/customer-management-api/credential/$personId"

    val headers = Seq(
      "GG-Group-ID"    -> groupId,
      "GG-External-ID" -> externalId
    )

    http
      .patch(url"$url")
      .withBody(Json.obj())
      .setHeader(headers: _*)
      .execute(throwOnFailure(readEitherOf(readUnit)), ec)
  }
}
