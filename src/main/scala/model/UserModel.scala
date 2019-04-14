package model

import org.bson.types.ObjectId

case class User(id: ObjectId = new ObjectId(),
                person: Person,
                phoneNumber: PhoneNumber,
                email: String,
                role: String,
                password: String,
                isAuthenticated: Boolean = false,
                orders: Option[List[Order]]
               )

case class Person(firstName: String, lastName:String)

case class PhoneNumber(name:String, number:String, imei:String)
