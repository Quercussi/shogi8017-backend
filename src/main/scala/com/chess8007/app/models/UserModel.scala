package com.chess8007.app.models

case class UserModel(userId: String, username: String)

object UserModel {
  def fromUserModelWithPassword(userModelWithPassword: UserModelWithPassword): UserModel = {
    UserModel(userModelWithPassword.userId, userModelWithPassword.username)
  }
}

case class UserModelWithPassword(userId: String, username: String, password: String)