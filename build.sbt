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
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;views.*;config.*;poc.view.*;" +
      "poc.config.*;.*(AuthService|BuildInfo|Routes).*;businessrates.authorisation.config.*;" +
      "businessrates.authorisation.models.*",
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val playSettings = Seq(
  routesImport ++= Seq("businessrates.authorisation.models.PermissionType")
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(playSettings ++ scoverageSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(PlayKeys.playDefaultPort := 9525)
  .settings(majorVersion := 0)
  .settings(
    libraryDependencies ++= compileDependencies ++ testDependencies,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest)(base => Seq(base / "it")),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false
  )
  .settings(resolvers ++= Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.jcenterRepo
  ))

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests map { test =>
    Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
  }

lazy val compileDependencies = Seq(
  ws,
  "uk.gov.hmrc"   %% "auth-client"            % "2.16.0-play-25",
  "uk.gov.hmrc"   %% "microservice-bootstrap" % "10.4.0" exclude ("uk.gov.hmrc", "play-authorisation_2.11"),
  "org.typelevel" %% "cats-core"              % "0.8.1",
  "uk.gov.hmrc"   %% "play-reactivemongo"     % "6.4.0"
)

lazy val testDependencies = Seq(
  "uk.gov.hmrc"            %% "hmrctest"           % "3.3.0"             % "test",
  "org.scalatest"          %% "scalatest"          % "3.0.6"             % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0"             % "test",
  "org.pegdown"            % "pegdown"             % "1.6.0"             % "test",
  "com.typesafe.play"      %% "play-test"          % PlayVersion.current % "test",
  "org.mockito"            % "mockito-core"        % "2.2.9"             % "test",
  "com.github.tomakehurst" % "wiremock"            % "2.5.1"             % "test",
  "org.scalacheck"         %% "scalacheck"         % "1.13.4"            % "test"
)

addCommandAlias("precommit", ";scalafmt;test:scalafmt;coverage;test;coverageReport")

