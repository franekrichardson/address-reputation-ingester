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

package services.fetch

import java.net.URL

import com.github.sardine.{DavResource, Sardine}
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatestplus.play.PlaySpec
import org.specs2.mock.Mockito
import services.model.StatusLogger
import uk.co.hmrc.logging.StubLogger

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class SardineWrapperTest extends PlaySpec with Mockito {

  import StubDavResource._

  val base = "http://somedavserver.com:81/webdav"
  val baseUrl = new URL(base + "/")

  val productResources = List[DavResource](
    dir("/webdav/", "webdav"),
    dir("/webdav/abi/", "abi"),
    dir("/webdav/abp/", "abp")
  )
  val abiEpochResources = List[DavResource](
    dir("/webdav/abi/", "abi")
  )
  val abpEpochResources = List[DavResource](
    dir("/webdav/abp/", "abp"),
    dir("/webdav/abp/38/", "38"),
    dir("/webdav/abp/39/", "39")
  )
  val abpE38VariantResources = List[DavResource](
    dir("/webdav/abp/38/", "38"),
    dir("/webdav/abp/38/full/", "full")
  )
  val abpE39VariantResources = List[DavResource](
    dir("/webdav/abp/39/", "39"),
    dir("/webdav/abp/39/full/", "full")
  )

  class Context(zipMedia: String, txtMedia: String) {
    val logger = new StubLogger()
    val status = new StatusLogger(logger)
    val sardine = mock[Sardine]
    val sardineFactory = mock[SardineFactory2]
    when(sardineFactory.begin("username", "password")) thenReturn sardine

    val file38Resources = List[DavResource](
      dir("/webdav/abp/38/full/", "full"),
      file("/webdav/abp/38/full/DVD1.zip", "DVD1.zip", zipMedia),
      file("/webdav/abp/38/full/DVD1.txt", "DVD1.txt", txtMedia)
    )
    val file39Resources = List[DavResource](
      dir("/webdav/abp/39/full/", "full"),
      file("/webdav/abp/39/full/DVD1.zip", "DVD1.zip", zipMedia),
      file("/webdav/abp/39/full/DVD1.txt", "DVD1.txt", txtMedia),
      file("/webdav/abp/39/full/DVD2.zip", "DVD2.zip", zipMedia),
      file("/webdav/abp/39/full/DVD2.txt", "DVD2.txt", txtMedia)
    )
  }

  "find available" should {
    "discover a tree of files using standard media types" in {
      new Context("application/zip", "text/plain") {
        // given
        when(sardine.list(base + "/")) thenReturn productResources.asJava
        when(sardine.list(base + "/abi/")) thenReturn abiEpochResources.asJava
        when(sardine.list(base + "/abp/")) thenReturn abpEpochResources.asJava
        when(sardine.list(base + "/abp/38/")) thenReturn abpE38VariantResources.asJava
        when(sardine.list(base + "/abp/39/")) thenReturn abpE39VariantResources.asJava
        when(sardine.list(base + "/abp/38/full/")) thenReturn file38Resources.asJava
        when(sardine.list(base + "/abp/39/full/")) thenReturn file39Resources.asJava
        val finder = new SardineWrapper(baseUrl, "username", "password", status, sardineFactory)
        // when
        val root = finder.exploreRemoteTree
        // then
        root must be(WebDavTree(
          WebDavFile(new URL(base + "/"), "webdav", isDirectory = true, files = List(
            WebDavFile(new URL(base + "/abi/"), "abi", isDirectory = true),
            WebDavFile(new URL(base + "/abp/"), "abp", isDirectory = true, files = List(
              WebDavFile(new URL(base + "/abp/38/"), "38", isDirectory = true, files = List(
                WebDavFile(new URL(base + "/abp/38/full/"), "full", isDirectory = true, files = List(
                  WebDavFile(new URL(base + "/abp/38/full/DVD1.zip"), "DVD1.zip", isZipFile = true),
                  WebDavFile(new URL(base + "/abp/38/full/DVD1.txt"), "DVD1.txt", isPlainText = true)
                ))
              )),
              WebDavFile(new URL(base + "/abp/39/"), "39", isDirectory = true, files = List(
                WebDavFile(new URL(base + "/abp/39/full/"), "full", isDirectory = true, files = List(
                  WebDavFile(new URL(base + "/abp/39/full/DVD1.zip"), "DVD1.zip", isZipFile = true),
                  WebDavFile(new URL(base + "/abp/39/full/DVD1.txt"), "DVD1.txt", isPlainText = true),
                  WebDavFile(new URL(base + "/abp/39/full/DVD2.zip"), "DVD2.zip", isZipFile = true),
                  WebDavFile(new URL(base + "/abp/39/full/DVD2.txt"), "DVD2.txt", isPlainText = true)
                ))
              ))
            ))
          ))))
      }
    }

    "discover a tree of files using the file extensions" in {
      new Context("application/octet-stream", "application/octet-stream") {
        // given
        when(sardine.list(base + "/")) thenReturn productResources.asJava
        when(sardine.list(base + "/abi/")) thenReturn abiEpochResources.asJava
        when(sardine.list(base + "/abp/")) thenReturn abpEpochResources.asJava
        when(sardine.list(base + "/abp/38/")) thenReturn abpE38VariantResources.asJava
        when(sardine.list(base + "/abp/39/")) thenReturn abpE39VariantResources.asJava
        when(sardine.list(base + "/abp/38/full/")) thenReturn file38Resources.asJava
        when(sardine.list(base + "/abp/39/full/")) thenReturn file39Resources.asJava
        val finder = new SardineWrapper(baseUrl, "username", "password", status, sardineFactory)
        // when
        val root = finder.exploreRemoteTree
        // then
        root must be(WebDavTree(
          WebDavFile(new URL(base + "/"), "webdav", isDirectory = true, files = List(
            WebDavFile(new URL(base + "/abi/"), "abi", isDirectory = true),
            WebDavFile(new URL(base + "/abp/"), "abp", isDirectory = true, files = List(
              WebDavFile(new URL(base + "/abp/38/"), "38", isDirectory = true, files = List(
                WebDavFile(new URL(base + "/abp/38/full/"), "full", isDirectory = true, files = List(
                  WebDavFile(new URL(base + "/abp/38/full/DVD1.zip"), "DVD1.zip", isZipFile = true),
                  WebDavFile(new URL(base + "/abp/38/full/DVD1.txt"), "DVD1.txt", isPlainText = true)
                ))
              )),
              WebDavFile(new URL(base + "/abp/39/"), "39", isDirectory = true, files = List(
                WebDavFile(new URL(base + "/abp/39/full/"), "full", isDirectory = true, files = List(
                  WebDavFile(new URL(base + "/abp/39/full/DVD1.zip"), "DVD1.zip", isZipFile = true),
                  WebDavFile(new URL(base + "/abp/39/full/DVD1.txt"), "DVD1.txt", isPlainText = true),
                  WebDavFile(new URL(base + "/abp/39/full/DVD2.zip"), "DVD2.zip", isZipFile = true),
                  WebDavFile(new URL(base + "/abp/39/full/DVD2.txt"), "DVD2.txt", isPlainText = true)
                ))
              ))
            ))
          ))))
      }
    }
  }
}

