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

import businessrates.authorisation.models.PropertyLink
import play.api.libs.json.{JsDefined, JsValue}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost}

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinking @Inject()(val http: HttpGet with HttpPost)(implicit ec: ExecutionContext) extends ServicesConfig {

  def linkedProperties(organisationId: Int)(implicit hc: HeaderCarrier): Future[Seq[PropertyLink]] = {
    val url = baseUrl("property-linking") + s"/property-links/$organisationId"
    http.GET[Seq[PropertyLink]](s"$url")
  }

}
