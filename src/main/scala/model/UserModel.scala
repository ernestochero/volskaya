package model

import org.bson.types.ObjectId

case class User(id: ObjectId = new ObjectId(),
                person: Person,
                email: String,
                role: String,
                password: String,
                cyclistStatus: Option[Boolean])

case class Person(firstname: String,
                  lastname:String,
                  phoneNumber: Option[String] = None,
                  imei: Option[String] = None)
