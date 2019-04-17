package models

import org.bson.types.ObjectId

case class User(id: ObjectId = new ObjectId(),
                person: Person,
                device: Device,
                email: String,
                role: String,
                password: String,
                isAuthenticated: Boolean,
                orders: Option[List[Order]]
               )

case class Person(firstName: String, lastName:String)

case class Device(name:String, number:String, imei:String, token: Option[String])
