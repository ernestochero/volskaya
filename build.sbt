name := "volskaya"

version := "0.1"

description := "GraphQL server written with sangria - Volskaya."

scalaVersion := "2.12.6"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.sangria-graphql" %% "sangria" % "1.4.2",
  "org.sangria-graphql" %% "sangria-slowlog" % "0.1.8",
  "org.sangria-graphql" %% "sangria-circe" % "1.2.1",
  "org.sangria-graphql" %% "sangria-play-json" % "1.0.5",
  
  "com.typesafe.akka" %% "akka-http" % "10.1.3",
  "de.heikoseeberger" %% "akka-http-circe" % "1.21.0",
  
  "io.circe" %%	"circe-core" % "0.9.3",
  "io.circe" %% "circe-parser" % "0.9.3",
  "io.circe" %% "circe-optics" % "0.9.3",

  "joda-time" % "joda-time" % "2.10.1",
  "org.joda" % "joda-convert" % "2.2.0",
  
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.1.0",
)

