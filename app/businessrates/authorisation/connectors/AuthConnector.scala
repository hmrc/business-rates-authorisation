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

package businessrates.authorisation.connectors

import javax.inject.Inject

import businessrates.authorisation.models.{Authority, GovernmentGatewayIds}
import cats.data.OptionT
import cats.implicits._
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, Upstream4xxResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthConnector @Inject() (val http: HttpGet) extends ServicesConfig {
  lazy val url = baseUrl("auth")

  def getGovernmentGatewayIds(implicit hc: HeaderCarrier) = {
    (for {
      authority <- OptionT(loadAuthority)
      ids <- OptionT(getIds(authority))
      userDetails <- OptionT(getUserDetails(authority))
    } yield {
      GovernmentGatewayIds(
        (ids \ "externalId").as[String],
        (userDetails \ "groupIdentifier").as[String]
      )
    }).value
  }

  private def loadAuthority(implicit hc: HeaderCarrier) = {
    http.GET[Option[Authority]](s"$url/auth/authority") recover {
      case Upstream4xxResponse(_, 401, _, _) => None
    }
  }

  private def getIds(authority: Authority)(implicit hc: HeaderCarrier) = authority.ids match {
    case Some(idsUri) => http.GET[Option[JsValue]](url + idsUri)
    case None => Future.successful(None)
  }

  private def getUserDetails(authority: Authority)(implicit hc: HeaderCarrier) = authority.userDetailsLink match {
    case Some(userDetailsUri) => http.GET[Option[JsValue]](userDetailsUri)
    case None => Future.successful(None)
  }
}