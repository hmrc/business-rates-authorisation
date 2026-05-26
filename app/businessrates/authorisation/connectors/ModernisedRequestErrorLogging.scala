/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.Logging
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.{ExecutionContext, Future}

trait ModernisedRequestErrorLogging extends Logging {

  protected def logModernisedErrorResponse[A](
        response: Future[A],
        errorInfo: => Seq[(String, String)],
        url: => String
  )(implicit ec: ExecutionContext): Future[A] =
    response.recoverWith { case error: UpstreamErrorResponse =>
      logError(error.statusCode, errorInfo, url)
      Future.failed(error)
    }

  private def logError(statusCode: Int, errorInfo: Seq[(String, String)], url: String): Unit = {
    val errorInfoString = (Seq(
      "statusCode" -> statusCode.toString
    ) ++ errorInfo :+ ("url" -> url))
      .map { case (key, value) => s"$key=$value" }
      .mkString(" ")

    logger.warn(s"ModernisedError $errorInfoString")
  }
}
