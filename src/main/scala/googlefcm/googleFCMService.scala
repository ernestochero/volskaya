package googlefcm

import googleMaps.GoogleFCMContext
import googlefcm.model.SendNotification
import zio._
import commons.Transformers._
package object googleFCMService {
  type GoogleFCMServiceType = Has[GoogleFCMService.Service]
  object GoogleFCMService {
    trait Service {
      def sendVerificationCode(
        verificationCode: String,
        phoneNumber: String,
        contextFCM: GoogleFCMContext
      ): Task[SendNotification]
    }
    def sendVerificationCode(
      verificationCode: String,
      phoneNumber: String,
      contextFCM: GoogleFCMContext
    ): ZIO[GoogleFCMServiceType, Throwable, SendNotification] =
      ZIO.accessM[GoogleFCMServiceType](
        _.get.sendVerificationCode(verificationCode, phoneNumber, contextFCM)
      )

    val live: Layer[Nothing, GoogleFCMServiceType] = ZLayer.succeed {
      new Service {
        override def sendVerificationCode(verificationCode: String,
                                          phoneNumber: String,
                                          contextFCM: GoogleFCMContext): Task[SendNotification] =
          SendNotificationApi.sendNotificationCode(verificationCode, phoneNumber, contextFCM).toTask
      }
    }
  }
}
