package modules

import googleMapsService.GoogleFCMContext
import googlefcmservice.SendNotificationApi
import zio.{ RIO, ZIO }
import commons.Transformers._
import googlefcmservice.model.SendNotification
import modules.GoogleFCMModule._
import zio.console.Console
trait GoogleFCMModule {
  val googleFCMModule: Service[Any]
}
object GoogleFCMModule {
  final case class GoogleFCMService(googleFCMContext: GoogleFCMContext) {
    implicit val context: GoogleFCMContext = googleFCMContext
    def SendVerificationCode(verificationCode: String,
                             phoneNumber: String): RIO[Console, SendNotification] =
      SendNotificationApi.sendNotificationCode(verificationCode, phoneNumber).toRIO
  }
  trait Service[R] {
    def googleFCMService(
      googleFCMContext: GoogleFCMContext
    ): ZIO[R, Throwable, GoogleFCMService]
  }

  trait Live extends GoogleFCMModule {
    override val googleFCMModule: Service[Any] = (googleFCMContext: GoogleFCMContext) =>
      ZIO.succeed(GoogleFCMService(googleFCMContext))
  }

  object factory extends Service[GoogleFCMModule] {
    override def googleFCMService(
      googleFCMContext: GoogleFCMContext
    ): ZIO[GoogleFCMModule, Throwable, GoogleFCMService] = ZIO.accessM[GoogleFCMModule](
      _.googleFCMModule.googleFCMService(googleFCMContext)
    )
  }

}
