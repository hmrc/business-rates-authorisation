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

import businessrates.authorisation.models.{Authority, GovernmentGatewayDetails, UserDetails}
import cats.data.OptionT
import cats.implicits._
import com.google.inject.name.Named
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream4xxResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthConnector @Inject() (@Named("simpleWSHttp") http: WSHttp) extends ServicesConfig {
  lazy val url = baseUrl("auth")

  def getGovernmentGatewayDetails(implicit hc: HeaderCarrier): Future[Option[GovernmentGatewayDetails]] = {
    (for {
      authority <- OptionT(loadAuthority)
      eventualIds = OptionT(getIds(authority))
      eventualUserDetails = OptionT(getUserDetails(authority))

      ggIds <- for {
        ids <- eventualIds
        userDetails <- eventualUserDetails
      } yield GovernmentGatewayDetails((ids \ "externalId").as[String], userDetails.groupIdentifier, userDetails.affinityGroup)
    } yield ggIds).value
  }

  private def loadAuthority(implicit hc: HeaderCarrier) = {
    http.GET[Option[Authority]](s"$url/auth/authority") recover {
      case Upstream4xxResponse(_, 401, _, _) => None
    }
  }

  private def getIds(authority: Authority)(implicit hc: HeaderCarrier) = authority.ids match {
    case Some(idsUri) => http.GET[Option[JsValue]](s"$url$idsUri")
    case None => Future.successful(None)
  }

  private def getUserDetails(authority: Authority)(implicit hc: HeaderCarrier) = authority.userDetailsLink match {
    case Some(userDetailsUri) => http.GET[Option[UserDetails]](userDetailsUri)
    case None => Future.successful(None)
  }
}
