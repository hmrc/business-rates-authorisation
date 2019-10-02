/*
 * Copyright 2019 HM Revenue & Customs
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

package businessrates.authorisation.action

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import businessrates.authorisation.auth.{Principal, RequestWithPrincipal}

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedActionBuilder @Inject()(
                                            override val authConnector: AuthConnector
                                          )(implicit override val executionContext: ExecutionContext)
  extends ActionBuilder[RequestWithPrincipal]
    with AuthorisedFunctions
    with Results {

  def invokeBlock[A](request: Request[A], block: RequestWithPrincipal[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, None, Some(request))

    authorised().retrieve(v2.Retrievals.externalId and v2.Retrievals.groupIdentifier) {
      case externalId ~ groupIdentifier =>
        (externalId, groupIdentifier) match {
          case (Some(exId), Some(grpId)) => block(RequestWithPrincipal(request, Principal(exId, grpId)))
          case (Some(_), None) => Future.successful(Unauthorized(Json.obj("errorCode" -> "NO_GROUP_ID")))
          case (None, Some(_)) => Future.successful(Unauthorized(Json.obj("errorCode" -> "NO_EXTERNAL_ID")))
          case (None, None) => Future.successful(Unauthorized(Json.obj("errorCode" -> "NO_EXTERNAL_ID_OR_GROUP_ID")))
        }
    }
  }
}
