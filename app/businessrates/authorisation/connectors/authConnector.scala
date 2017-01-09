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

import play.api.libs.json.Json
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthConnector @Inject() (val http: HttpGet) extends  ServicesConfig {


  def getGGGroupId()(implicit hc: HeaderCarrier) = {
    val url = baseUrl("auth") + s"/auth/authority"

    val groupId = http.GET(url).map(resp => {
      (Json.parse(resp.body) \ "userDetailsLink").as[String]
    })
      .flatMap(userDetailsLink => http.GET(userDetailsLink))
      .map(resp => {
        val js = Json.parse(resp.body)
        (js \ "groupIdentifier").as[String]
      })
    groupId
  }

}
