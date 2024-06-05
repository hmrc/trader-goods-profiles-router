import sbt._

object AppDependencies {

  private val catsVersion      = "2.6.1"
  private val bootstrapVersion = "8.4.0"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % bootstrapVersion,
    "org.typelevel" %% "cats-core"                 % catsVersion,
    "commons-validator" % "commons-validator" % "1.9.0"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "org.mockito" %% "mockito-scala"          % "1.17.31"        % Test
  )

  val it = Seq.empty
}
