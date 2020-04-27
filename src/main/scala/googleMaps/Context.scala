package googleMaps

trait Context
case class GoogleMapsContext(apiKey: String)           extends Context
case class GoogleFCMContext(to: String, token: String) extends Context
