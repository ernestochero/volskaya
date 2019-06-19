package volskayaSystem

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import googleMapsService.{ContextFCM, ContextGoogleMaps}
import logging.LoggingActor
import mongodb.Mongo
import user.UserManager

object VolskayaActorSystem {
  implicit val system = ActorSystem("sangria-server")
  implicit val materializer = ActorMaterializer()

  val googleMapsContext = ContextGoogleMaps(apiKey = "AIzaSyCXK3faSiD-RBShPD2TK1z1pRRpRaBdYtg")
  val fcmContext = ContextFCM(to = "cwcramwMhOo:APA91bG-p6fxc9EDUo8BD5MBk5y4zo04QF1Hi8DQ8frc3z38SmI1a4SGOc0TSkilJeMp_wALf17NRBVxUi51GLk2EYikjXfbRwy-ngjXT9lHkGk-iPCnMqBtW8wLxF2V51_oU38jPAlA",
    token = "key=AAAANyt87aU:APA91bFQjPaK7WRgEdzArxyuafUZFWZ0HR6LtFJWuc1q9Y6IrCu1sbgo2dU-7ywZNSIsqEdMkaISbkCs1nSZIaT3pKFwT7YaGsOm4gtHRsqrGMRuT9qzLDnQdt3mwLFBePij08xoAnex")

  val userManagementActor = system.actorOf(Props(classOf[UserManager], Mongo.usersCollection, googleMapsContext, fcmContext), "userManagementActor")
  // val volskayaLoggingActor = system.actorOf(Props(classOf[LoggingActor]), "volskayaLogginActor") // implement this in the future
}
