name := "dict-tools"

scalaVersion := "2.12.6"

// The library versions should be as mutually compatible as possible - else there will be weird runtime errors.
// We just use whatever we found compatible with akka-http-core in scala-utils_2.12
val akkaVersion = "2.5.19"
val akkaHttpVersion = "10.1.7"


resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3"
  ,"ch.qos.logback" % "logback-core" % "1.2.3"
  ,"org.json4s" % "json4s-ast_2.12" % "3.6.1"
  ,"org.json4s" % "json4s-native_2.12" % "3.6.1"
  ,"com.typesafe.akka" %% "akka-actor" % akkaVersion  // We use Akka Actor model for concurrent processing.
  ,"org.apache.commons" % "commons-csv" % "1.5"
  ,"org.apache.commons" % "commons-compress" % "1.18"
  , "com.github.sanskrit-coders" % "indic-transliteration_2.12" % "1.31"
  , "com.github.sanskrit-coders" % "StarDict" % "1.1"
  , "com.github.sanskrit-coders" % "scala-utils_2.12" % "0.5"
).map(_.exclude("org.slf4j", "*"))

//insert one without exclusion
libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.3"
)

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

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
