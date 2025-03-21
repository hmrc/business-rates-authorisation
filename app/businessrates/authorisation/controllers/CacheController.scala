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

package businessrates.authorisation.controllers

import businessrates.authorisation.repositories.AccountsCache
import com.google.inject.Inject
import play.api.mvc.Results.EmptyContent
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class CacheController @Inject() (cache: AccountsCache, controllerComponents: ControllerComponents)(implicit
      ec: ExecutionContext
) extends BackendController(controllerComponents) {

  def clearCache: Action[AnyContent] =
    Action.async { implicit request =>
      val header = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      header.sessionId.fold(Future.successful(Ok(EmptyContent()))) { sid =>
        cache.drop(sid.value) map { _ =>
          Ok(EmptyContent())
        }
      }
    }
}
