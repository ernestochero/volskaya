package googleMapsService

trait Context
case class ContextGoogleMaps(apiKey: String) extends Context
case class ContextFCM(to: String, token: String) extends Context