package com.chess8007.app.services

import cats.effect.IO
import com.chess8007.app.JwtConfig
import com.chess8007.app.errors.IncorrectUsernameOrPassword
import com.chess8007.app.jwtClaimModels.UserClaimModel
import com.chess8007.app.models.UserModel
import com.chess8007.app.repository.UserRepository
import com.chess8007.app.routes.{UserLoginPayload, UserLoginResponse}
import pdi.jwt.{JwtAlgorithm, JwtCirce}

class AuthenticationService(jwtConfig: JwtConfig, userRepository: UserRepository) {
  private val jwtAlgorithm = JwtAlgorithm.fromString(jwtConfig.algorithm.getOrElse("HS256"))
  private val userService = UserService.of(userRepository)

  private def encodeUserModel(userModel: UserModel, secret: String) = {
    val userClaimModel = UserClaimModel.of(userModel)
    val claim = userClaimModel.asJwtClaim(jwtConfig.accessTokenTtlSeconds)
    JwtCirce.encode(claim, secret, jwtAlgorithm)
  }

  private def getAccessToken(userModel: UserModel): String = {
    encodeUserModel(userModel, jwtConfig.accessTokenSecret)
  }

  private def getRefreshToken(userModel: UserModel): String = {
    encodeUserModel(userModel, jwtConfig.refreshTokenSecret)
  }

  def getUserToken(userModel: UserModel): UserLoginResponse = {
    val userClaimModel = UserClaimModel.of(userModel)
    val accessClaim = userClaimModel.asJwtClaim(jwtConfig.accessTokenTtlSeconds)
    val accessTokenExpiry = accessClaim.expiration.getOrElse(0L)
    val accessToken = getAccessToken(userModel)
    val refreshClaim = userClaimModel.asJwtClaim(jwtConfig.refreshTokenTtlSeconds)
    val refreshTokenExpiry = refreshClaim.expiration.getOrElse(0L)
    val refreshToken = getAccessToken(userModel)

    UserLoginResponse(
      accessToken = accessToken,
      accessTokenExpiry = accessTokenExpiry,
      refreshToken = refreshToken,
      refreshTokenExpiry = refreshTokenExpiry,
    )
  }

  def loginUser(payload: UserLoginPayload): IO[Either[Throwable, UserLoginResponse]] = {
    userService.authenticateUser(payload).map {
      case Left(error) => Left(error)
      case Right(user) => user match
        case None => Left(IncorrectUsernameOrPassword)
        case Some(userModel) => Right(getUserToken(userModel))
    }
  }
}

object AuthenticationService {
  def of(jwtConfig: JwtConfig, userRepository: UserRepository): AuthenticationService = new AuthenticationService(jwtConfig, userRepository)
}

