package repository

import org.mongodb.scala._
import models.{User, UserCyclist, UserDomain, UserProducer}
import org.mongodb.scala.bson.{BsonString, ObjectId}

import scala.concurrent.{ExecutionContext, Future}

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

  def updateEmail(userDomain: UserDomain) = {
    (userDomain.id, userDomain.email) match {
      case (Some(id), Some(email)) =>
        repository.updateEmail(new ObjectId(id), email).flatMap{
          case user: User => Future.successful(s"Updated Correctly : ${user._id.toHexString}")
          case _ => Future.successful(s"Failed to updated the email")
        }
      case (_, _) =>
        Future.successful("Failed : Incorrect Parameters")
    }
  }

  def updateUserType(userDomain: UserDomain) = {
    (userDomain.id, userDomain.userProducer, userDomain.userCyclist) match {
      case (Some(id),Some(userProducer), None) =>
        repository.updateUserProducer(new ObjectId(id), userProducer).flatMap{
          case user: User => Future.successful(s"Updated Correctly : ${user._id.toHexString}")
          case _ => Future.successful(s"Failed to updated the userProducer")
        }
      case (Some(id), None, Some(userCyclist)) =>
        repository.updateUserCyclist(new ObjectId(id), userCyclist).flatMap{
          case user: User => Future.successful(s"Updated Correctly : ${user._id.toHexString}")
          case _ => Future.successful(s"Failed to updated the userCyclist")
        }
      case (_, _, _) => Future.successful("Failed : Incorrect Parameters")
    }
  }


}