package repository

import org.mongodb.scala._
import models.{User, UserCyclist, UserDomain, UserProducer}
import org.mongodb.scala.bson.{ObjectId}

import scala.concurrent.{ExecutionContext, Future}
import models.VolskayaMessages._

class UserRepository(collection: MongoCollection[User])(implicit ec:ExecutionContext) {

  def saveUser(user: User): Future[User] = {
    collection
      .insertOne(user)
      .head()
      .map { _ => user}
  }

  def getAllUsers: Future[Seq[User]] = { collection.find().toFuture() }

  def updateEmail(_id: ObjectId, email:String): Future[User] = {
    val filter = Document("_id" -> _id)
    val update = Document("$set" ->  Document("email" -> email))
    collection.findOneAndUpdate(filter, update).head()
  }

  def updatePassword(_id: ObjectId, password:String): Future[User] = {
    val filter = Document("_id" -> _id)
    val update = Document("$set" ->  Document("password" -> password))
    collection.findOneAndUpdate(filter, update).head()
  }

  def updateUserCyclist(_id: ObjectId, userCyclist: UserCyclist): Future[User] = {
    val filter = Document("_id" -> _id)
    val fields = Document(
      "firstName" -> userCyclist.firstName,
      "lastName" -> userCyclist.lastName,
      "dni" -> userCyclist.dni
    )
    val update = Document("$set" -> Document("userCyclist" -> fields))
    collection.findOneAndUpdate(filter, update).head()
  }

  def updateUserProducer(_id: ObjectId, userProducer: UserProducer): Future[User] = {
    val fields = Document(
      "nameCompany" -> userProducer.nameCompany,
      "address" -> userProducer.address,
      "phone" -> userProducer.phone,
      "ruc" -> userProducer.ruc
    )
    val filter = Document("_id" -> _id)
    val update = Document("$set" ->  Document("userProducer" -> fields))
    collection.findOneAndUpdate(filter, update).head()
  }
}

class UserRepo(repository: UserRepository)(implicit ec: ExecutionContext) {

  def allUsers = repository.getAllUsers.map( user => user.map(_.asDomain ))

  def saveUser(userDomain: UserDomain) = {
    repository.saveUser(userDomain.asResource).map(_.asDomain)
  }

  //TODO:  here I call other functions to operate the database

  def updateEmail(userDomain: UserDomain): Future[VolskayaResponse] = {
    (userDomain.id, userDomain.email) match {
      case (Some(id), Some(email)) =>
        repository.updateEmail(new ObjectId(id), email).flatMap {
          case user: User => Future.successful(VolskayaSuccessfulResponse(fieldId = models.EmailField))
          case _ => Future.successful(VolskayaFailedResponse(fieldId = models.EmailField))
        }
      case (_, _) =>
        Future.successful(VolskayaIncorrectParameters())
    }
  }

  def updateUserType(userDomain: UserDomain):Future[VolskayaResponse] = {
    (userDomain.id, userDomain.userProducer, userDomain.userCyclist) match {
      case (Some(id),Some(userProducer), None) =>
        repository.updateUserProducer(new ObjectId(id), userProducer).flatMap {
          case user: User => Future.successful(VolskayaSuccessfulResponse(fieldId = models.UserProducerField))
          case _ => Future.successful(VolskayaFailedResponse(fieldId = models.UserProducerField))
        }
      case (Some(id), None, Some(userCyclist)) =>
        repository.updateUserCyclist(new ObjectId(id), userCyclist).flatMap {
          case user: User => Future.successful(VolskayaSuccessfulResponse(fieldId = models.UserCyclistField))
          case _ => Future.successful(VolskayaFailedResponse(fieldId = models.UserCyclistField))
        }
      case (_, _, _) =>
        Future.successful(VolskayaIncorrectParameters())
    }
  }

  def updatePassword(userDomain: UserDomain):Future[VolskayaResponse] = {
    (userDomain.id, userDomain.password) match {
      case (Some(id), Some(password)) =>
        repository.updatePassword(new ObjectId(id), password).flatMap {
          case user: User => Future.successful(VolskayaSuccessfulResponse(fieldId = models.PasswordField))
          case _ => Future.successful(VolskayaFailedResponse(fieldId = models.PasswordField))
        }
      case (_, _) =>
        Future.successful(VolskayaIncorrectParameters())
    }
  }

}