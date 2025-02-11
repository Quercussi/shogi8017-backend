package com.shogi8017.app.repository

case class CreateUserPayload(username: String, hashedPassword: String)
case class GetUserPayload(userId: String)
case class FindUserByUsernamePayload(username: String)