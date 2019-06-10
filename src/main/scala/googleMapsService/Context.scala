package googleMapsService

trait Context
case class ContextGoogleMaps(apiKey: String) extends Context
case class ContextFCM(to: String, key: String) extends Context