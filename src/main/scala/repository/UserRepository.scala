package repository

import org.mongodb.scala._
import models.{ User, UserDomain }

import scala.concurrent.{ExecutionContext, Future}

class UserRepository(collection: MongoCollection[User])(implicit ec:ExecutionContext) {

  def saveUser(user: User): Future[User] = {
    collection
      .insertOne(user)
      .head()
      .map { _ => user}
  }

  def getAllUsers: Future[Seq[User]] = { collection.find().toFuture() }

}

class UserRepo(repository: UserRepository)(implicit ec: ExecutionContext) {

  def allUsers = repository.getAllUsers.map( user => user.map(_.asDomain ))

  def saveUser(userDomain: UserDomain) = {
    repository.saveUser(userDomain.asResource).map(_.asDomain)
  }

}