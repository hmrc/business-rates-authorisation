import play.sbt.routes.RoutesKeys._
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "business-rates-authorisation"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()

  override lazy val playSettings = Seq(
    routesImport ++= Seq("businessrates.authorisation.models.PermissionType")
  )
}

private object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val microserviceBootstrapVersion = "9.1.0"
  private val domainVersion = "4.1.0"
  private val hmrcTestVersion = "3.3.0"
  private val scalaTestVersion = "2.2.6"
  private val pegdownVersion = "1.6.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % microserviceBootstrapVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "org.typelevel" %% "cats-core" % "0.8.1",
    "uk.gov.hmrc" %% "play-reactivemongo" % "5.2.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "reactivemongo-test" % "2.0.0" % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % "2.2.9",
        "com.github.tomakehurst" % "wiremock" % "2.5.1" % "test",
        "org.scalacheck" %% "scalacheck" % "1.13.4" % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}
