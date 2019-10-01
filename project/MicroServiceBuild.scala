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

  private val microserviceBootstrapVersion = "10.4.0"
  private val hmrcTestVersion = "3.3.0"
  private val scalaTestVersion = "3.0.6"
  private val pegdownVersion = "1.6.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "auth-client" % "2.16.0-play-25",
  "uk.gov.hmrc" %% "microservice-bootstrap" % microserviceBootstrapVersion,
    "org.typelevel" %% "cats-core" % "0.8.1",
    "uk.gov.hmrc" %% "play-reactivemongo" % "6.4.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope,
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
