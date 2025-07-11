import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.6"

lazy val microservice = Project("trader-goods-profiles-router", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    PlayKeys.playDefaultPort := 10904,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s",
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*Routes.*",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
  .settings(CodeCoverageSettings.settings *)


lazy val it = project
  .enablePlugins(PlayScala)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)

addCommandAlias("testAndCoverage", ";clean;coverage;test;it/test;coverageReport")
addCommandAlias("prePR", ";scalafmt;test:scalafmt;testAndCoverage")
addCommandAlias("preMerge", ";scalafmtCheckAll;testAndCoverage")