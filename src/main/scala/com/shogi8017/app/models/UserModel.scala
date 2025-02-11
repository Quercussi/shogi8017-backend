package com.shogi8017.app.models

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

case class UserModel(userId: String, username: String)

object UserModel {
  def fromUserModelWithPassword(userModelWithPassword: UserModelWithPassword): UserModel = {
    UserModel(userModelWithPassword.userId, userModelWithPassword.username)
  }
  
  implicit val userModelEncoder: Encoder[UserModel] = deriveEncoder
}