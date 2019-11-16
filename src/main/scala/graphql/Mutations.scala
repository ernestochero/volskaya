package graphql
import caliban.CalibanError.ExecutionError
import caliban.schema.Annotations.GQLDescription
import models.User
import models.VolskayaMessages.VolskayaResult
import zio.ZIO
import zio.console.Console
case class updatePasswordArg(id: String, oldPassword: String, newPassword: String)
case class Mutations(
  @GQLDescription("Volskaya return a user by id")
  updatePassword: updatePasswordArg => ZIO[Console, ExecutionError, VolskayaResult[Option, String]],
  @GQLDescription("Volskaya create a user with just a role")
  insertUser: insertRole => ZIO[Console, ExecutionError, VolskayaResult[Option, User]]
)
