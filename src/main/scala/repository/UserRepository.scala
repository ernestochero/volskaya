package repository

import org.mongodb.scala._
import models.User

import scala.concurrent.{ExecutionContext, Future}

class UserRepository(collection: MongoCollection[User])(implicit ec:ExecutionContext) {

  def saveUser(user: User): Future[User] = {
    collection
      .insertOne(user)
      .head()
      .map { _ => user}
  }

}
