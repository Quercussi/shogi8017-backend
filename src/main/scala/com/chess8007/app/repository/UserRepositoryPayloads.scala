package com.chess8007.app.repository

case class CreateUserPayload(username: String, hashedPassword: String)
case class FindUserByUsernamePayload(username: String)