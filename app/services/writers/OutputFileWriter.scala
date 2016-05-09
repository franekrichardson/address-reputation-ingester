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

package services.writers

import java.io.{OutputStreamWriter, _}
import java.util.zip.GZIPOutputStream

import config.ConfigHelper._
import play.api.Play._
import services.model.{StateModel, StatusLogger}
import uk.co.hmrc.address.osgb.DbAddress

object OutputFileWriterHelper {
  val outputFolder = new File(replaceHome(mustGetConfigString(current.mode, current.configuration, "app.files.outputFolder")))
  outputFolder.mkdirs()
}


class OutputFileWriter(var model: StateModel, statusLogger: StatusLogger) extends OutputWriter {

  val fileRoot = model.collectionBaseName
  val outputFile = new File(OutputFileWriterHelper.outputFolder, s"$fileRoot.txt.gz")

  private val bufSize = 32 * 1024
  private val outfile = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile), bufSize))
  private val outCSV = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outfile), bufSize))

  private var count = 0

  override def output(a: DbAddress) {
    // scalastyle:off
    outCSV.println(a.toString)
    count += 1
  }

  override def close(): StateModel = {
    if (outCSV.checkError()) {
      statusLogger.warn(s"Failed whilst writing to $outputFile")
      model = model.copy(hasFailed = true)
    }
    outCSV.close()
    println(s"*** document count = $count")
    model
  }
}


class OutputFileWriterFactory extends OutputWriterFactory {
  override def writer(model: StateModel, statusLogger: StatusLogger, settings: WriterSettings) =
    new OutputFileWriter(model, statusLogger)
}
