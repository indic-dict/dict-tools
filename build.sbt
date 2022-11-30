name := "dict-tools"
ThisBuild / versionScheme := Some("strict")
scalaVersion := "2.13.10"

// The library versions should be as mutually compatible as possible - else there will be weird runtime errors.
// We just use whatever we found compatible with akka-http-core in scala-utils_2.13
val akkaVersion = "2.7.0"


resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers +=
  "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"


libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3"
  ,"ch.qos.logback" % "logback-core" % "1.2.3"
  ,"org.json4s" %% "json4s-native-core" % "4.1.0-M2"
  ,"com.typesafe.akka" %% "akka-actor" % akkaVersion  // We use Akka Actor model for concurrent processing.
  ,"org.apache.commons" % "commons-csv" % "1.6"
  ,"org.apache.commons" % "commons-compress" % "1.18"
  , "com.github.sanskrit-coders" % "StarDict" % "1.1"
  // If the below is outdated, an option is to just comment the line out and include the latest jars in the lib folder.
  , ("com.github.sanskrit-coders" %% "indic-transliteration" % "1.33")
  , ("com.github.sanskrit-coders" %% "scala-utils" % "1.25.0")
  ,  "com.47deg" %% "github4s" % "0.31.2"
  , "com.github.scopt" %% "scopt" % "4.0.1"
  , "com.ibm.icu" % "icu4j" % "68.2"
  , "org.tukaani" % "xz" % "1.0"
//  ,"com.github.david-bouyssie" % "sqlite4s_native0.4_2.13" % "0.4.0"
).map(_.exclude("org.slf4j", "*"))

//insert one without exclusion
libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.3"
)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % "test"


assembly / assemblyOutputPath := file("bin/artifacts/dict-tools.jar")
assembly / mainClass := Some("stardict_sanskrit.commandInterface")


publishMavenStyle := true
publishTo := sonatypePublishToBundle.value


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
