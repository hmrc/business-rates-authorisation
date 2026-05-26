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

import ch.qos.logback.classic.Level
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Logger
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import scala.concurrent.{ExecutionContext, Future}

class ModernisedRequestErrorLoggingSpec extends AnyWordSpec with Matchers with ScalaFutures with LogCapturing {

  private class TestConnector extends ModernisedRequestErrorLogging {
    def execute[A](response: Future[A], errorInfo: => Seq[(String, String)], url: => String)(implicit
          ec: ExecutionContext
    ): Future[A] =
      logModernisedErrorResponse(response, errorInfo, url)
  }

  private val connector = new TestConnector

  implicit val ec: ExecutionContext = ExecutionContext.global

  "logModernisedErrorResponse" should {
    "log a WARN with relevant IDs when Modernised returns an error" in {
      val upstreamError = UpstreamErrorResponse("Internal Server Error", 500)
      val endpoint = "http://modernised/customer-management-api/credential/123"

      withCaptureOfLoggingFrom(Logger(classOf[TestConnector])) { logs =>
        whenReady(
          connector
            .execute(
              response = Future.failed(upstreamError),
              errorInfo = Seq(
                "personId"   -> "123",
                "groupId"    -> "group-id",
                "externalId" -> "external-id"
              ),
              url = endpoint
            )
            .failed
        ) { failed =>
          failed shouldBe upstreamError
        }

        val warnLogs = logs.filter(_.getLevel == Level.WARN)
        warnLogs should have size 1

        val message = warnLogs.head.getMessage
        message should include("ModernisedError")
        message should include("statusCode=500")
        message should include("personId=123")
        message should include("groupId=group-id")
        message should include("externalId=external-id")
        message should include(s"url=$endpoint")
      }
    }
  }
}
