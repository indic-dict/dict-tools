name := "dict-tools"

scalaVersion := "2.12.3"

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "0.9.29"
  ,"ch.qos.logback" % "logback-core" % "0.9.29"
  ,"org.json4s" % "json4s-ast_2.12" % "3.5.2"
  ,"org.json4s" % "json4s-native_2.12" % "3.5.2"
  ,"org.apache.commons" % "commons-csv" % "1.4"
  ,"com.github.sanskrit-coders" % "indic-transliteration_2.12" % "1.10"
  ,"com.github.sanskrit-coders" % "StarDict" % "1.1"
  //    ,"com.github.sanskrit-coders" % "sanskrit-lttoolbox" % "0.1"
  //  ,"com.github.sanskrit-coders" % "db-interface" % "1.8"
)

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.4"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

scmInfo := Some(
  ScmInfo(
    url("https://github.com/sanskrit-coders/dict-tools"),
    "scm:git@github.com:sanskrit-coders/dict-tools.git"
  )
)

assemblyOutputPath in assembly := file("bin/artifacts/dict-tools.jar")
mainClass in assembly := Some("stardict_sanskrit.commandInterface")


useGpg := true
publishMavenStyle := true
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  releaseStepCommand("assembly"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)
