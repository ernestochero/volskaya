name := "volskaya"

version := "0.1"

description := "GraphQL server written with caliban - Volskaya."

scalaVersion := "2.12.10"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
scalacOptions ++= Seq("-deprecation", "-feature")
scalacOptions ++= Seq("-Ypartial-unification")
val akkaVersion = "2.5.19"
val akkaHttpVersion = "10.1.8"
val calibanVersion = "0.7.6"
val circeVersion = "0.12.2"
val mongoDriverVersion = "2.7.0"
val pureConfigVersion = "0.12.1"

val calibanDependencies = Seq(
  "com.github.ghostdogpr" %% "caliban" % calibanVersion,
  "com.github.ghostdogpr" %% "caliban-http4s" %  calibanVersion,
)
val circeDependencies = Seq(
  "io.circe"      %% "circe-parser"        %  circeVersion,
  "io.circe"      %% "circe-derivation"    %  "0.12.0-M7",
  "io.circe" %%	"circe-core" % "0.13.0",
  "io.circe" %% "circe-optics" % "0.13.0"
)

val mongoDriverDependencies = Seq(
  "org.mongodb.scala" %% "mongo-scala-driver" % mongoDriverVersion
)
val pureConfigDependencies = Seq(
  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion
)

libraryDependencies ++=
  Seq(
  "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor"  % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "de.heikoseeberger" %% "akka-http-circe" % "1.21.0",
  "org.log4s" %% "log4s" % "1.8.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "com.pauldijou" %% "jwt-core" % "4.2.0"
) ++ mongoDriverDependencies ++ circeDependencies ++ calibanDependencies ++ pureConfigDependencies

Revolver.settings
enablePlugins(JavaAppPackaging)