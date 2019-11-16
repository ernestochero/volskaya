name := "volskaya"

version := "0.1"

description := "GraphQL server written with caliban - Volskaya."

scalaVersion := "2.12.10"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
scalacOptions ++= Seq("-deprecation", "-feature")
scalacOptions ++= Seq("-Ypartial-unification")
val akkaVersion = "2.5.19"
val akkaHttpVersion = "10.1.8"
val calibanVersion = "0.2.0"
val circeVersion = "0.12.2"
val zioVersion = "2.0.0.0-RC6"
val http4sVersion = "0.20.6"
val mongoDriverVersion = "2.7.0"
val pureConfigVersion = "0.12.1"

val calibanDependencies = Seq(
  "com.github.ghostdogpr" %% "caliban" % calibanVersion,
  "com.github.ghostdogpr" %% "caliban-http4s" %  calibanVersion
)
val circeDependencies = Seq(
  "io.circe"      %% "circe-parser"        %  circeVersion,
  "io.circe"      %% "circe-derivation"    %  "0.12.0-M7",
  "io.circe" %%	"circe-core" % "0.9.3",
  "io.circe" %% "circe-optics" % "0.9.3"
)
val zioDependencies = Seq(
  "dev.zio"       %% "zio-interop-cats"    % zioVersion
)
val http4sDependencies = Seq(
  "org.http4s"    %% "http4s-dsl"          % http4sVersion,
  "org.http4s"    %% "http4s-circe"        % http4sVersion,
  "org.http4s"    %% "http4s-blaze-server" % http4sVersion,
)
val mongoDriverDependencies = Seq(
  "org.mongodb.scala" %% "mongo-scala-driver" % mongoDriverVersion
)
val pureConfigDependencies = Seq(
  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion
)


libraryDependencies ++=
  Seq(
  "org.sangria-graphql" %% "sangria" % "1.4.2",
  "org.sangria-graphql" %% "sangria-slowlog" % "0.1.8",
  "org.sangria-graphql" %% "sangria-circe" % "1.2.1",
  "org.sangria-graphql" %% "sangria-play-json" % "1.0.5",
  "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor"  % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "de.heikoseeberger" %% "akka-http-circe" % "1.21.0",
  "joda-time" % "joda-time" % "2.10.1",
  "org.joda" % "joda-convert" % "2.2.0",
  "log4j" % "log4j" % "1.2.17",
) ++ mongoDriverDependencies ++ http4sDependencies ++ zioDependencies ++ circeDependencies ++ calibanDependencies ++ pureConfigDependencies

Revolver.settings
enablePlugins(JavaAppPackaging)