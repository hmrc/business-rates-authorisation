/*
 * Copyright 2016 HM Revenue & Customs
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

import businessrates.authorisation.models.APIValuationHistory
import play.api.libs.json._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPut}

import scala.concurrent.ExecutionContext.Implicits.global

class VmvConnector @Inject() (val http: HttpGet) extends  ServicesConfig  {

  def getValuationHistory(uarn: Long)(implicit hc: HeaderCarrier) = {
    http.GET[JsValue](s"${baseUrl("external-business-rates-data-platform")}/ndrlist/valuation_history/$uarn").map(js =>{
      (js \ "NDRListValuationHistoryItems").as[List[APIValuationHistory]]
    })
  }

}
