package graphql
import caliban.schema.Annotations.GQLDescription
import models.UserManagementExceptions.VolskayaAPIException
import models.VolskayaMessages.VolskayaResult
import userCollection.UserCollectionService.UserCollectionServiceType
import zio.ZIO
case class updatePasswordArg(id: String, oldPassword: String, newPassword: String)
case class Mutations(
  @GQLDescription("Volskaya return a user by id")
  updatePassword: updatePasswordArg => ZIO[UserCollectionServiceType,
                                           VolskayaAPIException,
                                           VolskayaResult[Option, String]]
)
