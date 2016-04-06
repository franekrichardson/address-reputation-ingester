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

package controllers

import play.api.mvc.{AnyContent, Action}
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.io.Source
import play.api.Play._


object PingController extends PingController


trait PingController extends BaseController {

  val stream = getClass.getResourceAsStream("/provenance.json")
  val versionInfo = Source.fromInputStream(stream).mkString
  stream.close()

  def ping(): Action[AnyContent] = Action { request =>
    Ok(versionInfo).withHeaders(CONTENT_TYPE -> "application/json")
  }
}
