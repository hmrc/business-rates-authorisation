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

import businessrates.authorisation.connectors.BackendConnector.UpdateCredentialsSuccess
import businessrates.authorisation.models._
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BstBackendConnector @Inject()(val http: VOABackendWSHttp, servicesConfig: ServicesConfig)
    extends BackendConnector {

  lazy val backendUrl: String = servicesConfig.baseUrl("voa-bst")

  val groupAccountsUrl = s"$backendUrl/customer-management-api/organisation"
  val individualAccountsUrl: String = s"$backendUrl/customer-management-api/person"

  private def NotFound[T]: PartialFunction[Throwable, Option[T]] = {
    case _: NotFoundException => None
  }

  override def getOrganisationByGGId(
        ggId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Organisation]] =
    getOrganisation(ggId, "governmentGatewayGroupId")

  override def getPerson(
        externalId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Person]] = {
    implicit val apiFormat: Reads[Person] = Person.apiFormat
    http.GET[Option[Person]](s"$individualAccountsUrl?governmentGatewayExternalId=$externalId") recover NotFound[Person]
  }

  private def getOrganisation(id: String, paramName: String)(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[Option[Organisation]] = {
    implicit val apiFormat: Reads[Organisation] = Organisation.apiFormat
    http.GET[Option[Organisation]](s"$groupAccountsUrl?$paramName=$id") recover NotFound[Organisation]
  }

  override def updateCredentials(personId: String, groupId: String, externalId: String)(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[UpdateCredentialsSuccess.type] = {
    val url = s"$backendUrl/customer-management-api/credential/$personId"

    val headers = Seq(
      "GG-Group-ID"    -> groupId,
      "GG-External-ID" -> externalId,
    )

    http.PATCH(url, Json.obj(), headers).map { _ =>
      UpdateCredentialsSuccess //Map any OK case to an UpdateCredentialsSuccess as any non 2xx will return a failed future
    }
  }
}
