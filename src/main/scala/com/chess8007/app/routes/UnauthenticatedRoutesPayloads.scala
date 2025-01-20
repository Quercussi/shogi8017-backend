package com.chess8007.app.routes

case class UserSignUpPayload(username: String, password: String) 
case class UserSignUpResponse(userId: String, username: String)
