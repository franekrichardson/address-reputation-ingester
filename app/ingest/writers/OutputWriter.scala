/*
 *
 *  * Copyright 2016 HM Revenue & Customs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package ingest.writers

import java.util.Date

import ingest.algorithm.Algorithm
import services.model.{StateModel, StatusLogger}
import uk.gov.hmrc.address.osgb.DbAddress

import scala.concurrent.ExecutionContext

trait OutputWriter {
  def existingTargetThatIsNewerThan(date: Date): Option[String]

  def begin()

  def output(a: DbAddress)

  def end(completed: Boolean): StateModel
}

trait OutputWriterFactory {
  def writer(model: StateModel, statusLogger: StatusLogger, settings: WriterSettings, ec: ExecutionContext): OutputWriter
}


case class WriterSettings(bulkSize: Int, loopDelay: Int, algorithm: Algorithm = Algorithm())

object WriterSettings {
  val default = WriterSettings(1, 0, Algorithm())
}
