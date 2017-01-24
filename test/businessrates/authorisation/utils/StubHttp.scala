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

package businessrates.authorisation.utils

import businessrates.authorisation.config.VOABackendWSHttp
import play.api.libs.json.Writes
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost}

object StubHttp extends VOABackendWSHttp {
  override def doGet(url: String)(implicit hc: HeaderCarrier) = ???

  override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit wts: Writes[A], hc: HeaderCarrier) = ???

  override def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier) = ???

  override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier) = ???

  override def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier) = ???

  override val hooks = NoneRequired
}
