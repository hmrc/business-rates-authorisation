import play.core.PlayVersion
import play.sbt.PlayImport.{PlayKeys, _}
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "business-rates-authorisation"

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
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .settings(playSettings ++ scoverageSettings: _*)
  .settings(scalaSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(PlayKeys.playDefaultPort := 9525)
  .settings(majorVersion := 0)
  .settings(scalaVersion := "2.13.8")
  .settings(
    targetJvm := "jvm-1.8",
    Test / fork := true,
    libraryDependencies ++= compileDependencies ++ testDependencies,
    retrieveManaged := true
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    IntegrationTest / fork := false,
    IntegrationTest / unmanagedSourceDirectories := {(IntegrationTest / baseDirectory)(base => Seq(base / "it"))}.value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / testGrouping := oneForkedJvmPerTest((IntegrationTest / definedTests).value),
    IntegrationTest / parallelExecution := false
  )
  .settings(resolvers += Resolver.jcenterRepo)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) = tests.map { test =>
  Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
}
val bootstrapVersion = "7.15.0"

lazy val compileDependencies = Seq(
  ws,
  "uk.gov.hmrc"       %% "bootstrap-backend-play-28"    % bootstrapVersion,
  "org.typelevel"     %% "cats-core"                    % "2.9.0",
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"           % "0.74.0"
)

lazy val testDependencies = Seq(
  "uk.gov.hmrc"            %% "bootstrap-test-play-28" % bootstrapVersion % "test, it",
  "org.scalatest"          %% "scalatest"          % "3.0.8"             % "test,it",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0"             % "test,it",
  "org.pegdown"            % "pegdown"             % "1.6.0"             % "test,it",
  "com.typesafe.play"      %% "play-test"          % PlayVersion.current % "test,it",
  "org.mockito"            % "mockito-core"        % "3.4.6"             % "test,it",
  "org.scalatestplus"      %% "mockito-3-4"        % "3.2.9.0"           % "test,it",
  "com.github.tomakehurst" % "wiremock-jre8"       % "2.23.2"            % "test,it",
  "org.scalacheck"         %% "scalacheck"         % "1.14.0"            % "test,it",
  "com.vladsch.flexmark"   % "flexmark-all"        % "0.35.10"           % "test,it"
)

ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
addCommandAlias("precommit", ";scalafmt;test:scalafmt;coverage;test;coverageReport")
