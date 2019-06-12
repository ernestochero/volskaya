package repository

import akka.actor.ActorSystem
import googleMapsService.{Context, ContextFCM, ContextGoogleMaps, DistanceMatrixApi}
import org.mongodb.scala._
import models._
import org.mongodb.scala.bson.ObjectId
import googleMapsService.model.{Distance, DistanceMatrix, Units}
import googlefcmservice.SendNotificationApi

import scala.concurrent.{ExecutionContext, Future}
import models.VolskayaMessages._
import org.mongodb.scala.result.UpdateResult
import akka.event.Logging
import com.sun.corba.se.spi.ior.ObjectKey

class UserRepository(collection: MongoCollection[User])(implicit ec:ExecutionContext) {

  def saveUser(user: User): Future[User] = {
    collection
      .insertOne(user)
      .head()
      .map { _ => user}
  }

  def getAllUsers: Future[Seq[User]] = { collection.find().toFuture() }

  def verifyLogin(email:String, password:String): Future[User] = {
    val filter = Document("email" -> email, "password" -> password)
    collection.find(filter).first().head()
  }

  def getUser(id: ObjectId): Future[User] = {
    val filter = Document("_id" -> id)
    collection.find(filter).first().head()
  }

  def updateEmail(_id: ObjectId, email:String): Future[User] = {
    val filter = Document("_id" -> _id)
    val update = Document("$set" ->  Document("email" -> email))
    collection.findOneAndUpdate(filter, update).head()
  }

  def updatePassword(_id: ObjectId, oldPassword:String, newPassword: String): Future[UpdateResult] = {
    val filter = Document("_id" -> _id, "password" -> oldPassword)
    val update = Document("$set" -> Document("password" -> newPassword))
    collection.updateOne(filter, update).head()
  }

  def updatePersonalInformation(_id: ObjectId, personalInformation: PersonalInformation): Future[User] = {
    val filter = Document("_id" -> _id)
    val fields = Document(
      "firstName" -> personalInformation.firstName,
      "lastName" -> personalInformation.lastName,
      "dni" -> personalInformation.dni
    )
    val update = Document("$set" -> Document("personalInformation" -> fields))
    collection.findOneAndUpdate(filter, update).head()
  }

  def addFavoriteSite(_id: ObjectId, favoriteSite: FavoriteSite) = {
    val favoriteSiteField = Document(
      "coordinate" -> Document("latitude" -> favoriteSite.coordinate.latitude, "longitude" -> favoriteSite.coordinate.longitude),
      "name" -> favoriteSite.name,
      "address" -> favoriteSite.address
    )
    val filter = Document("_id" -> _id)
    val update = Document("$push" -> Document("favoriteSites" -> favoriteSiteField))
    collection.updateOne(filter, update).head()
  }

  // TODO: improve this part , maybe stored the confirmationCode on REDIS
  def saveConfirmationCode(_id: ObjectId, confirmationCode: String) = {
    val filter = Document("_id" -> _id)
    val update = Document("$set" -> Document("confirmationCode" -> confirmationCode))
    collection.updateOne(filter, update).head()
  }

  def checkCode(_id: ObjectId, code: String) = {
    val filter = Document("_id" -> _id, "confirmationCode" -> code)
    collection.find(filter).head()
  }

}

class UserRepo(repository: UserRepository)(implicit ec: ExecutionContext) {
  // TODO this context should be move to another place
  val context = ContextGoogleMaps(apiKey = "AIzaSyCXK3faSiD-RBShPD2TK1z1pRRpRaBdYtg")

  def allUsers = repository.getAllUsers.map( user => user.map(_.asDomain ))

  def saveUser(userDomain: UserDomain) = {
    repository.saveUser(userDomain.asResource).map(_.asDomain)
  }

  //TODO:  here I call other functions to operate the database
  // UserDomain(None,None,None,None,None,None,None,None) create a static val
  def getUser(id: String): Future[VolskayaGetUserResponse] = {
    repository.getUser(new ObjectId(id)).flatMap { user =>
      Future.successful(VolskayaGetUserResponse(Some(user.asDomain), VolskayaSuccessResponse(responseMessage = getSuccessGetMessage(models.UserField)) ))
    }.recoverWith {
      case exception => Future.successful(VolskayaGetUserResponse(None, VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(errorMsg = exception.getMessage))))
    }
  }

  def updatePassword(_id: String, oldPassword: String, newPassword: String): Future[VolskayaResponse] = {
    repository.updatePassword(new ObjectId(_id), oldPassword, newPassword).flatMap {
      case res:UpdateResult if (res.getMatchedCount == 1) && res.wasAcknowledged() =>
        Future.successful(VolskayaSuccessResponse(responseMessage = getSuccessUpdateMessage(fieldId = PasswordField)))
      case _ =>
        Future.successful(VolskayaFailedResponse(responseMessage = getFailedUpdateMessage(fieldId = PasswordField)))
    }.recoverWith {
      case exception => Future.successful(VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(errorMsg = exception.getMessage)))
    }
  }

  // TODO : implement the calculate of price with googleMaps API
  def calculatePriceRoute(coordinateStart: Coordinate, coordinateFinish: Coordinate): Future[VolskayaGetPriceResponse] = {
    val origins = coordinateStart.toString :: Nil
    val destinations = coordinateFinish.toString :: Nil

    //TODO optimize this function when we have a geoZone
    def calculateByDistance(distanceMeters:Int): Double = distanceMeters match {
      case v if v > 0 && v < 2800 => 4.0
      case v if v > 2900 && v < 3800 => 5.0
      case v if v > 3900 && v < 4800 => 6.0
      case v if v > 4900 && v < 5800 => 7.0
      case v if v > 5900 && v < 6800 => 8.0
      case v if v > 6900 && v < 7800 => 9.0
      case _ => 10.0
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
        Future.successful(VolskayaGetPriceResponse(price.headOption, VolskayaSuccessResponse(responseMessage = getSuccessCalculateMessage(models.PriceFieldId))))
      case _ => Future.successful(VolskayaGetPriceResponse(None, VolskayaFailedResponse(responseMessage = getFailedCalculateMessage(models.PriceFieldId))))
    }.recoverWith {
      case exception => Future.successful(VolskayaGetPriceResponse(None, VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(errorMsg = exception.getMessage))))
    }
  }

  def login(email:String, password: String): Future[VolskayaLoginResponse] = {
      repository.verifyLogin(email, password).flatMap { user =>
        Future.successful(VolskayaLoginResponse(Some(user._id.toHexString), VolskayaSuccessResponse(responseMessage = getSuccessLoginMessage(models.UserField))))
      }.recoverWith {
        case exception  => Future.successful(VolskayaLoginResponse(None,
          VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(errorMsg = exception.getMessage))))
      }
  }

  def sendCode(code: String, phoneNumber: String): Future[VolskayaResponse] = {
    val context = ContextFCM(to = "cwcramwMhOo:APA91bG-p6fxc9EDUo8BD5MBk5y4zo04QF1Hi8DQ8frc3z38SmI1a4SGOc0TSkilJeMp_wALf17NRBVxUi51GLk2EYikjXfbRwy-ngjXT9lHkGk-iPCnMqBtW8wLxF2V51_oU38jPAlA",
        token = "key=AAAANyt87aU:APA91bFQjPaK7WRgEdzArxyuafUZFWZ0HR6LtFJWuc1q9Y6IrCu1sbgo2dU-7ywZNSIsqEdMkaISbkCs1nSZIaT3pKFwT7YaGsOm4gtHRsqrGMRuT9qzLDnQdt3mwLFBePij08xoAnex")

    val sendCodeResult = SendNotificationApi.sendNotificationCode(code, phoneNumber)(context)
    sendCodeResult.flatMap { result =>
        if(result.success == 1) {
          Future.successful(VolskayaSuccessResponse(responseMessage = getSuccessSendcode(fieldId = VerificationCodeId)))
        } else {
          Future.successful(VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(errorMsg = s" Failure = ${result.failure}")))
        }
    }.recoverWith {
      case exception =>
        Future.successful(VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(errorMsg = exception.getMessage)))
    }
  }

  def checkCode(id: String, code: String) = {
    repository.checkCode(new ObjectId(id), code).flatMap {
      case user: User =>
        Future.successful(VolskayaSuccessResponse(responseMessage = getSuccessChecked(fieldId = VerificationCodeId)))
      case _ =>
        Future.successful(VolskayaFailedResponse(responseMessage = getFailedChecked(fieldId = VerificationCodeId)))
    }.recoverWith{
        case exception => Future.failed(exception)
      }
  }


  def register(email: String, password: String, phoneNumber: String) = {
    import scala.util.Random
    val device = Device(name = "", number = phoneNumber, imei = "")
    val user = User(email = Some(email), password = Some(password), device = Some(device))
    repository.saveUser(user).flatMap { user =>

      val confirmationCode = (1 to 6).foldLeft(""){(c,v) => s"$c${Random.nextInt(10)}"}
      val msgCodeGenerate = s"Tu codigo de verificacion es $confirmationCode "

      // TODO: Improbe this part with durable Actors
      (for {
        saveResult <- repository.saveConfirmationCode(user._id, confirmationCode)
        sendResult <- sendCode(msgCodeGenerate, phoneNumber)
      } yield {
        println(s"Save Confirmation Code Result : {$saveResult.toString} ")
        print(s"Send Confirmation Code Result : {${sendResult.responseCode}} {${sendResult.responseMessage}} ")
        // system.log.info("Save Confirmation Code Result : {} ", saveResult.toString)
        // system.log.info("Send Confirmation Code Result : {} {} ", sendResult.responseCode, sendResult.responseMessage)
      }).recover{
        case exception =>
          println(s"An error occurred {${exception.getMessage}} ")
          //system.log.error("An error occurred {} ", exception.getMessage)
      }

      Future.successful(VolskayaRegisterResponse(Some(user._id.toHexString),
        VolskayaSuccessResponse(responseMessage = getSuccessRegisteredMessage(models.UserField))))
    }.recoverWith {
      case exception =>  Future.successful(VolskayaRegisterResponse(None,
        VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(errorMsg = exception.getMessage))))
    }
  }

  def addFavoriteSite(id: String, favoriteSite: FavoriteSite) = {
    repository.addFavoriteSite(new ObjectId(id), favoriteSite).flatMap {
      case result: UpdateResult  if (result.getMatchedCount == 1) && result.wasAcknowledged() =>
        Future.successful(VolskayaSuccessResponse(responseMessage = getSuccessUpdateMessage(fieldId = FavoriteSiteFieldId)))
      case _ => Future.successful(VolskayaFailedResponse(responseMessage = getFailedUpdateMessage(fieldId = FavoriteSiteFieldId)))
    }.recoverWith {
      case exception => Future.successful(VolskayaFailedResponse(responseMessage = getDefaultErrorMessage(errorMsg = exception.getMessage)))
    }
  }

}