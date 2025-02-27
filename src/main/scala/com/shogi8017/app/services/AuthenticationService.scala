package com.shogi8017.app.services

import cats.data.EitherT
import cats.effect.IO
import com.shogi8017.app.JwtConfig
import com.shogi8017.app.exceptions.IncorrectUsernameOrPassword
import com.shogi8017.app.jwtClaimModels.UserClaimModel
import com.shogi8017.app.models.UserModel
import com.shogi8017.app.repository.UserRepository
import com.shogi8017.app.routes.{UserLoginPayload, UserLoginResponse, WebSocketResponsePayload}
import pdi.jwt.{JwtAlgorithm, JwtCirce}

class AuthenticationService(jwtConfig: JwtConfig, userRepository: UserRepository) {
  private val jwtAlgorithm = JwtAlgorithm.fromString(jwtConfig.algorithm.getOrElse("HS256"))
  private val userService = UserService.of(userRepository)

  private def encodeUserModel(userModel: UserModel, secret: String, ttlSeconds: Int) = {
    val userClaimModel = UserClaimModel.of(userModel)
    val claim = userClaimModel.asJwtClaim(ttlSeconds)
    JwtCirce.encode(claim, secret, jwtAlgorithm)
  }

  private def getAccessToken(userModel: UserModel): String = {
    encodeUserModel(userModel, jwtConfig.accessTokenSecret, jwtConfig.accessTokenTtlSeconds)
  }

  private def getRefreshToken(userModel: UserModel): String = {
    encodeUserModel(userModel, jwtConfig.refreshTokenSecret, jwtConfig.refreshTokenTtlSeconds)
  }

  private def getWebSocketAccessToken(userModel: UserModel): String = {
    encodeUserModel(userModel, jwtConfig.websocketAccessTokenSecret, jwtConfig.websocketAccessTokenTtlSeconds)
  }

  def getUserToken(userModel: UserModel): UserLoginResponse = {
    val userClaimModel = UserClaimModel.of(userModel)

    val accessClaim = userClaimModel.asJwtClaim(jwtConfig.accessTokenTtlSeconds)
    val accessTokenExpiry = accessClaim.expiration.getOrElse(0L)
    val accessToken = getAccessToken(userModel)

    val refreshClaim = userClaimModel.asJwtClaim(jwtConfig.refreshTokenTtlSeconds)
    val refreshTokenExpiry = refreshClaim.expiration.getOrElse(0L)
    val refreshToken = getRefreshToken(userModel)

    UserLoginResponse(
      accessToken = accessToken,
      accessTokenExpiry = accessTokenExpiry,
      refreshToken = refreshToken,
      refreshTokenExpiry = refreshTokenExpiry,
    )
  }

  def getUserWebsocketToken(userModel: UserModel): WebSocketResponsePayload = {
    val userClaimModel = UserClaimModel.of(userModel)

    val websocketAccessClaim = userClaimModel.asJwtClaim(jwtConfig.websocketAccessTokenTtlSeconds)
    val websocketAccessTokenExpiry = websocketAccessClaim.expiration.getOrElse(0L)
    val websocketAccessToken = getWebSocketAccessToken(userModel)

    WebSocketResponsePayload(
      websocketAccessToken = websocketAccessToken,
      websocketAccessTokenExpiry = websocketAccessTokenExpiry
    )
  }

  def loginUser(payload: UserLoginPayload): EitherT[IO, Throwable, UserLoginResponse] = {
    userService.authenticateUser(payload).flatMap {
      case None => 
        EitherT.leftT[IO, UserLoginResponse](IncorrectUsernameOrPassword)
      case Some(userModel) => 
        EitherT.rightT[IO, Throwable](getUserToken(userModel))
    }
  }
}

object AuthenticationService {
  def of(jwtConfig: JwtConfig, userRepository: UserRepository): AuthenticationService = new AuthenticationService(jwtConfig, userRepository)
}

