import play.sbt.PlayImport.{PlayKeys, _}
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

import uk.gov.hmrc.DefaultBuildSettings

val appName = "business-rates-authorisation"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.12"

ThisBuild / excludeDependencies ++= Seq(
  // As of Play 3.0, groupId has changed to org.playframework; exclude transitive dependencies to the old artifacts
  // Specifically affects play-json-extensions dependency
  ExclusionRule(organization = "com.typesafe.play")
)

lazy val scoverageSettings = {
  // Semicolon-separated list of regexs matching classes to exclude
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*Reverse.*;views.*;config.*;poc.view.*;" +
      "poc.config.*;.*(AuthService|BuildInfo|Routes).*;businessrates.authorisation.config.*;" +
      "businessrates.authorisation.models.*",
    ScoverageKeys.coverageMinimumStmtTotal := 81,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val playSettings = Seq()

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(playSettings ++ scoverageSettings: _*)
  .settings(scalaSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(PlayKeys.playDefaultPort := 9525)
  .settings(
    targetJvm := "jvm-11",
    Test / fork := true,
    libraryDependencies ++= compileDependencies ++ testDependencies,
    retrieveManaged := true
  )
  .settings(resolvers += Resolver.jcenterRepo)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= testDependencies)
  .settings(Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(resolvers += Resolver.jcenterRepo)

val businessRatesValuesVersion = "3.0.0"
val bootstrapPlayVersion = "9.5.0"

lazy val compileDependencies = Seq(
  ws,
  "uk.gov.hmrc"       %% "bootstrap-backend-play-30"    % bootstrapPlayVersion,
  "org.typelevel"     %% "cats-core"                    % "2.12.0",
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"           % "2.3.0"
)

lazy val testDependencies = Seq(
  "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootstrapPlayVersion % Test,
  "org.pegdown"            % "pegdown"                 % "1.6.0"              % Test,
  "org.scalacheck"         %% "scalacheck"             % "1.18.1"             % Test
)

ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

addCommandAlias("precommit", ";coverage;scalafmt;test:scalafmt;it/test:scalafmt;test;it/test;coverageReport")
