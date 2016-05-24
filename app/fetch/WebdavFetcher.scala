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

package fetch

import java.io.File
import java.net.URL
import java.nio.file._

import com.github.sardine.{DavResource, Sardine}
import services.model.StatusLogger
import uk.co.bigbeeconsultants.http.util.DiagnosticTimer

import scala.collection.JavaConverters._

class WebdavFetcher(factory: SardineWrapper, val downloadFolder: File, status: StatusLogger) {

  // Downloads a specified set of remote files, marks them all with a completion marker (.done),
  // then returns the total bytes copied.
  def fetchList(product: OSGBProduct, outputPath: String, forceFetch: Boolean): List[DownloadItem] = {
    val outputDirectory = resolveAndMkdirs(outputPath)
    val sardine = factory.begin
    product.zips.map {
      webDavFile =>
        fetchFile(webDavFile.url, sardine, outputDirectory, forceFetch)
    }
  }

  // Searches for remote files, downloads them, marks them all with a completion marker (.done),
  // then returns the total bytes copied.
  // Note that this doesn't check the existence of completion marker files on the remote server.
  def fetchAll(url: String, outputPath: String, forceFetch: Boolean): List[DownloadItem] = {
    val outputDirectory = resolveAndMkdirs(outputPath)
    val sardine = factory.begin
    status.info("Listing {}.", url)
    val resources = sardine.list(url).asScala.toList.filterNot(_.isDirectory)
    resources.map {
      res =>
        val absoluteUrl = toUrl(url, res)
        fetchFile(absoluteUrl, sardine, outputDirectory, forceFetch)
    }
  }

  private def toUrl(base: String, res: DavResource): URL = {
    val myUrl = new URL(base)
    new URL(myUrl.getProtocol, myUrl.getHost, myUrl.getPort, res.getHref.getPath)
  }

  private def fetchFile(url: URL, sardine: Sardine, outputDirectory: File, forceFetch: Boolean): DownloadItem = {
    val file = fileOf(url)
    val outFile = ZipFile(outputDirectory, file)
    if (forceFetch || !outFile.exists || outFile.isIncomplete) {
      // pre-existing files are considered incomplete
      outFile.delete()

      val ff = if (forceFetch) " (forced)" else ""
      status.info(s"Fetching {} to $file$ff.", url)
      val dt = new DiagnosticTimer
      val fetched = doFetchFile(url, sardine, outFile)
      outFile.touchDoneFile()
      status.info(s"Fetched $file in {}.", dt)
      DownloadItem.fresh(fetched)

    } else {
      status.info(s"Already had $file.")
      DownloadItem.stale(outFile)
    }
  }

  private def doFetchFile(url: URL, sardine: Sardine, outFile: ZipFile): ZipFile = {
    val in = sardine.get(url.toExternalForm)
    try {
      Files.copy(in, outFile.toPath)
      outFile
    } finally {
      in.close()
    }
  }

  private def fileOf(url: URL): String = {
    val file = url.getPath
    val slash = file.lastIndexOf('/')
    file.substring(slash + 1)
  }

  private def resolveAndMkdirs(outputPath: String): File = {
    val outputDirectory = if (outputPath.nonEmpty) new File(downloadFolder, outputPath) else downloadFolder
    outputDirectory.mkdirs()
    outputDirectory
  }
}


case class DownloadItem(file: ZipFile, fresh: Boolean)

object DownloadItem {
  def fresh(f: ZipFile): DownloadItem = new DownloadItem(f, true)

  def stale(f: ZipFile): DownloadItem = new DownloadItem(f, false)
}


case class ZipFile(file: File) {
  def this(fpath: String) = this(new File(fpath))

  def product: String = file.getParentFile.getParentFile.getParentFile.getName

  def epoch: Int = file.getParentFile.getParentFile.getName.toInt

  def variant: String = file.getParentFile.getName

  val doneFile = new File(file.getParentFile, file.getName + ".done")

  def getName: String = file.getName

  def length: Long = file.length()

  def toPath: Path = file.toPath

  val isZipFile: Boolean = file.getName.toLowerCase.endsWith(".zip")

  def exists: Boolean = file.exists

  def isIncomplete: Boolean = !doneFile.exists || file.lastModified > doneFile.lastModified

  def delete() {
    file.delete()
    doneFile.delete()
  }

  def touchDoneFile() {
    Files.createFile(doneFile.toPath)
  }
}

object ZipFile {
  def apply(dir: File, name: String): ZipFile = new ZipFile(new File(dir, name))
}
