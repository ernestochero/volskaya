package repository

import googleMapsService.{Context, DistanceMatrixApi}
import org.mongodb.scala._
import models.{Coordinate, Route, User, UserCyclist, UserDomain, UserProducer}
import org.mongodb.scala.bson.ObjectId
import googleMapsService.model.{Distance, DistanceMatrix, Units}

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

  def verifyLogin(email:String, password:String): Future[Boolean] = {
    val filter = Document("email" -> email)
    collection.find(filter).first().head().map{ _.password.fold(false){ _ == password}}
  }

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
  // TODO this context should be move to another place
  val context = Context(apiKey = "AIzaSyCXK3faSiD-RBShPD2TK1z1pRRpRaBdYtg")

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
        }.recoverWith {
          case ex: NullPointerException => Future.successful(VolskayaUserNotExist(errorMsg = ex.getMessage))
          case exception  => Future.successful(VolskayaDefaultErrorMessage(errorMsg = exception.getMessage))
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
        }.recoverWith {
          case ex: NullPointerException => Future.successful(VolskayaUserNotExist(errorMsg = ex.getMessage))
          case exception  => Future.successful(VolskayaDefaultErrorMessage(errorMsg = exception.getMessage))
        }
      case (Some(id), None, Some(userCyclist)) =>
        repository.updateUserCyclist(new ObjectId(id), userCyclist).flatMap {
          case user: User => Future.successful(VolskayaSuccessfulResponse(fieldId = models.UserCyclistField))
          case _ => Future.successful(VolskayaFailedResponse(fieldId = models.UserCyclistField))
        }.recoverWith {
          case ex: NullPointerException => Future.successful(VolskayaUserNotExist(errorMsg = ex.getMessage))
          case exception  => Future.successful(VolskayaDefaultErrorMessage(errorMsg = exception.getMessage))
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
        }.recoverWith {
          case ex: NullPointerException => Future.successful(VolskayaUserNotExist(errorMsg = ex.getMessage))
          case exception  => Future.successful(VolskayaDefaultErrorMessage(errorMsg = exception.getMessage))
        }
      case (_, _) =>
        Future.successful(VolskayaIncorrectParameters())
    }
  }

  // TODO : implement the calculate of price with googleMaps API
  def calculatePriceRoute(route: Route): Future[List[Int]] = {
    val origins = route.coordinateA.toString :: Nil
    val destinations = route.coordinateB.toString :: Nil

    //TODO optimize this function when we have a geoZone
    def calculateByDistance(distanceMeters:Int): Int = distanceMeters match {
      case v if v > 0 && v < 2800 => 4
      case v if v > 2900 && v < 3800 => 5
      case v if v > 3900 && v < 4800 => 6
      case v if v > 4900 && v < 5800 => 7
      case v if v > 5900 && v < 6800 => 8
      case v if v > 6900 && v < 7800 => 9
      case _ => 10
    }

    val distanceMatrix = DistanceMatrixApi.getDistanceMatrix(origins, destinations, units = Some(Units.metric))(context)

    distanceMatrix.foreach(println(_))
    // TODO implement a better solution for this part
    distanceMatrix.flatMap {
      case dm:DistanceMatrix if dm.status == "OK" =>
        val price = for {
          row <- dm.rows
          element <- row.elements
        } yield {
          calculateByDistance(element.distance.value)
        }
        Future.successful(price)
      case _ => Future.failed(new Exception("error to calculate the price of route"))
    }
  }

  def login(maybeEmail:Option[String], maybePassword:Option[String]): Future[VolskayaResponse] = {
    (maybeEmail, maybePassword) match {
      case (Some(email), Some(password)) =>
        repository.verifyLogin(email, password).flatMap {
          case true => Future.successful(VolskayaSuccessfulLogin())
          case false =>  Future.successful(VolskayaFailedLogin())
        }.recoverWith {
          case ex: NullPointerException => Future.successful(VolskayaUserNotExist(errorMsg = ex.getMessage))
          case exception  => Future.successful(VolskayaDefaultErrorMessage(errorMsg = exception.getMessage))
        }
      case (_ , _ ) => Future.successful(VolskayaIncorrectParameters())
    }
  }


}