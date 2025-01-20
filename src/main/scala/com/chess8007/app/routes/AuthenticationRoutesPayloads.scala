package com.chess8007.app.routes

import io.circe.generic.auto._
import io.circe.syntax._

case class UserLoginPayload(username: String, password: String)
case class UserLoginResponse(accessToken: String, refreshToken: String, accessTokenExpiry: Long, refreshTokenExpiry: Long)

case class TokenRefreshPayload(username: String, password: String)
case class TokenRefreshResponse()