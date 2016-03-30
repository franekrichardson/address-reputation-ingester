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

import sbt._

object MicroServiceBuild extends Build with MicroService {
  import scala.util.Properties.envOrElse

  val appName = "address-reputation-ingester"
  val appVersion = envOrElse("ADDRESS_REPUTATION_INGESTER_VERSION", "999-SNAPSHOT")

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.PlayImport._
  import play.core.PlayVersion

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-reactivemongo" % "4.7.1",
    "uk.gov.hmrc" %% "microservice-bootstrap" % "4.2.1",
    "uk.gov.hmrc" %% "play-authorisation" % "3.1.0",
    "uk.gov.hmrc" %% "play-health" % "1.1.0",
    "uk.gov.hmrc" %% "play-url-binders" % "1.0.0",
    "uk.gov.hmrc" %% "play-config" % "2.0.1",
    "uk.gov.hmrc" %% "play-json-logger" % "2.1.1",
    "uk.gov.hmrc" %% "domain" % "3.3.0",
    "uk.gov.hmrc" %% "address-reputation-store" % "0.3.0" withSources()
      excludeAll ExclusionRule(organization = "org.reactivemongo"),
    "org.apache.ftpserver" % "ftpserver" % "1.0.5",
    "org.apache.commons" % "commons-compress" % "1.10",
    "commons-net" % "commons-net" % "3.4",
    "org.mongodb" %% "casbah" % "3.1.0"
  )

  trait TestDependencies {
    val scope = "test"

    def test: Seq[ModuleID]

    lazy val baseTestDependencies = Seq(
      "org.scalatest" %% "scalatest" % "2.2.4" % scope,
      "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % scope,
      "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.2" % scope,
      "org.pegdown" % "pegdown" % "1.4.2" % scope,
      "uk.gov.hmrc" %% "hmrctest" % "1.4.0" % scope,
      "org.scalatestplus" %% "play" % "1.2.0" % scope,
      "org.jsoup" % "jsoup" % "1.7.3" % scope,
      "org.mockito" % "mockito-all" % "1.10.19" % scope)
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = baseTestDependencies
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {
      override val scope = "it"
      override val test = baseTestDependencies ++ Seq(
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope)
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}

