package googlefcmservice.model

final case class ResultNotification(`message_id`: String)

final case class SendNotification(`multicast_id`: String,
                               success: Int,
                               failure: Int,
                              `canonical_ids`: Int,
                              results: List[ResultNotification])