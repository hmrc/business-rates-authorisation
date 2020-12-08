import play.core.PlayVersion
import play.sbt.PlayImport.{PlayKeys, _}
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.{SbtAutoBuildPlugin, _}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
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
    ScoverageKeys.coverageMinimum := 60,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val playSettings = Seq()

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(playSettings ++ scoverageSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(PlayKeys.playDefaultPort := 9525)
  .settings(majorVersion := 0)
  .settings(scalaVersion := "2.12.12")
  .settings(
    targetJvm := "jvm-1.8",
    fork in Test := true,
    libraryDependencies ++= compileDependencies ++ testDependencies,
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.0" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.7.0" % Provided cross CrossVersion.full
    ),
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := {(baseDirectory in IntegrationTest)(base => Seq(base / "it"))}.value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false
  )
  .settings(resolvers ++= Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.jcenterRepo
  ))

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) = tests.map { test =>
  Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
}

lazy val compileDependencies = Seq(
  ws,
  "uk.gov.hmrc"   %% "auth-client"          % "3.0.0-play-26",
  "uk.gov.hmrc"   %% "bootstrap-play-26"    % "1.15.0",
  "org.typelevel" %% "cats-core"            % "1.6.1",
  "uk.gov.hmrc"   %% "simple-reactivemongo" % "7.30.0-play-26"
)

lazy val testDependencies = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % "test",
  "org.scalatest"          %% "scalatest"          % "3.0.6"             % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0"             % "test",
  "org.pegdown"            % "pegdown"             % "1.6.0"             % "test",
  "com.typesafe.play"      %% "play-test"          % PlayVersion.current % "test",
  "org.mockito"            % "mockito-core"        % "2.27.0"            % "test",
  "com.github.tomakehurst" % "wiremock-jre8"       % "2.21.0"            % "test",
  "org.scalacheck"         %% "scalacheck"         % "1.13.4"            % "test"
)


scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature")
// silence all warnings on autogenerated files
scalacOptions += "-P:silencer:pathFilters=target/.*"
// Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}"

addCommandAlias("precommit", ";scalafmt;test:scalafmt;coverage;test;coverageReport")
