package graphql
import caliban.schema.Annotations.GQLDescription
import models.UserManagementExceptions.VolskayaAPIException
import models.VolskayaMessages.VolskayaResultSuccessResponse
import zio.ZIO
import zio.console.Console
case class updatePasswordArg(id: String, oldPassword: String, newPassword: String)
case class Mutations(
  @GQLDescription("Volskaya return a user by id")
  updatePassword: updatePasswordArg => ZIO[Console,
                                           VolskayaAPIException,
                                           VolskayaResultSuccessResponse[Option, String]]
)
