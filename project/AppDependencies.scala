import sbt._

object AppDependencies {

  private val catsVersion      = "2.13.0"
  private val bootstrapVersion = "9.13.0"

  val compile = Seq(
    "uk.gov.hmrc"      %% "bootstrap-backend-play-30" % bootstrapVersion,
    "org.typelevel"    %% "cats-core"                 % catsVersion,
    "commons-validator" % "commons-validator"         % "1.9.0",
    "com.beachape"     %% "enumeratum-play"           % "1.9.0"
  )

  val test = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"   % bootstrapVersion % Test,
    "org.scalatestplus" %% "mockito-4-11"            % "3.2.18.0"
  )

  val it = Seq.empty
}
