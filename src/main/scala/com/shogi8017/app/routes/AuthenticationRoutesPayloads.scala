package com.shogi8017.app.routes

import io.circe.generic.auto._
import io.circe.syntax._

case class UserLoginPayload(username: String, password: String)
case class UserLoginResponse(accessToken: String, refreshToken: String, accessTokenExpiry: Long, refreshTokenExpiry: Long)
object UserLoginResponse {
  def from(r: TokenRefreshResponse): UserLoginResponse = {
    UserLoginResponse(r.accessToken, r.refreshToken, r.accessTokenExpiry, r.refreshTokenExpiry)
  }
}

case class TokenRefreshPayload(refreshToken: String)
case class TokenRefreshResponse(accessToken: String, refreshToken: String, accessTokenExpiry: Long, refreshTokenExpiry: Long)
object TokenRefreshResponse {
  def from(l: UserLoginResponse): TokenRefreshResponse = {
    TokenRefreshResponse(l.accessToken, l.refreshToken, l.accessTokenExpiry, l.refreshTokenExpiry)
  }
}