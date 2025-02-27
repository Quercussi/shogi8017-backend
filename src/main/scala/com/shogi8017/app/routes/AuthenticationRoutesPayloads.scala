package com.shogi8017.app.routes

import com.shogi8017.app.models.UserModel
import io.circe.generic.auto.*

case class UserLoginPayload(username: String, password: String)
case class UserLoginResponse(user: UserModel, accessToken: String, refreshToken: String, accessTokenExpiry: Long, refreshTokenExpiry: Long)
object UserLoginResponse {
  def from(r: TokenRefreshResponse): UserLoginResponse = {
    UserLoginResponse(r.user, r.accessToken, r.refreshToken, r.accessTokenExpiry, r.refreshTokenExpiry)
  }
}

case class TokenRefreshPayload(refreshToken: String)
case class TokenRefreshResponse(user: UserModel, accessToken: String, refreshToken: String, accessTokenExpiry: Long, refreshTokenExpiry: Long)
object TokenRefreshResponse {
  def from(l: UserLoginResponse): TokenRefreshResponse = {
    TokenRefreshResponse(l.user, l.accessToken, l.refreshToken, l.accessTokenExpiry, l.refreshTokenExpiry)
  }
}

case class WebSocketRequestPayload()
case class WebSocketResponsePayload(websocketAccessToken: String, websocketAccessTokenExpiry: Long)
