resolvers += Classpaths.typesafeReleases

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")
addSbtPlugin("com.heroku" % "sbt-heroku" % "2.1.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.4")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")