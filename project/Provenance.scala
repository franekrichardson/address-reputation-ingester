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

import java.text.SimpleDateFormat
import java.util.Date

import sbt.Keys._
import sbt._

import scala.util.Properties._

object Provenance {

  def run(command: String): String = (command !!).trim

  def makeProvenanceSources(base: File): Seq[File] = {
    val tag = envOrElse("BUILD_TAG", "DEVELOPER")
    val number = envOrElse("BUILD_NUMBER", "")
    val id = envOrElse("BUILD_ID", "")
    val url = envOrElse("JOB_URL", "")
    val commit = run("git rev-parse HEAD").replace(':', '_')
    val remote = run("git remote")
    val branch = run("git rev-parse --abbrev-ref HEAD")
    val time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date())
    val file = base / "provenance.json"
    if (!file.exists()) {
      val text =
        s"""{
        |  "appName":     "${HmrcBuild.appName}",
        |  "version":     "${HmrcBuild.appVersion}",
        |  "buildTag":    "$tag",
        |  "buildNumber": "$number",
        |  "buildId":     "$id",
        |  "jobUrl":      "$url",
        |  "gitCommit":   "$commit",
        |  "gitBranch":   "$remote/$branch",
        |  "timestamp":   "$time"
        |}""".stripMargin
      IO.write(file, text)
    }
    Seq(file)
  }

  def provenanceTask = Def.task {
    val base = (resourceManaged in Sources).value
    makeProvenanceSources(base / "resources")
  }

  def setting: Setting[_] = resourceGenerators in Compile += provenanceTask.taskValue
}
