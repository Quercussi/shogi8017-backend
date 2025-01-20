package com.chess8007.app.services

import cats.effect.IO
import com.chess8007.app.JwtConfig
import com.chess8007.app.database.DatabaseResource
import com.chess8007.app.errors.IncorrectUsernameOrPassword
import com.chess8007.app.jwtClaimModels.{EmptyClaimModel, UserClaimModel}
import com.chess8007.app.models.UserModel
import com.chess8007.app.repository.UserRepository
import com.chess8007.app.routes.{UserLoginPayload, UserLoginResponse}
import pdi.jwt.{JwtAlgorithm, JwtCirce}

class AuthenticationService(jwtConfig: JwtConfig, userRepository: UserRepository) {
  private val jwtAlgorithm = JwtAlgorithm.fromString(jwtConfig.algorithm.getOrElse("HS256"))
  private val userService = UserService.of(userRepository)

  private def getAccessToken(userModel: UserModel): String = {
    val userClaimModel = UserClaimModel.of(userModel)
    val claim = userClaimModel.asJwtClaim(jwtConfig.accessTokenTtlSeconds)
    val token = JwtCirce.encode(claim, jwtConfig.accessTokenSecret, jwtAlgorithm)
    token
  }

  private def getRefreshToken(emptyClaimModel: EmptyClaimModel): String = {
    val claim = emptyClaimModel.asJwtClaim(jwtConfig.refreshTokenTtlSeconds)
    JwtCirce.encode(claim, jwtConfig.refreshTokenSecret, jwtAlgorithm)
  }

  def loginUser(payload: UserLoginPayload): IO[Either[Throwable, UserLoginResponse]] = {
    userService.authenticateUser(payload).map {
      case Left(error) => Left(error)
      case Right(user) => user match
        case None => Left(IncorrectUsernameOrPassword)
        case Some(userModel) =>
          val userClaimModel = UserClaimModel.of(userModel)
          val accessClaim = userClaimModel.asJwtClaim(jwtConfig.accessTokenTtlSeconds)
          val accessTokenExpiry = accessClaim.expiration.getOrElse(0L)
          val accessToken = getAccessToken(userModel)
  
          // TODO: recheck on refresh token model
          val emptyClaimModel = EmptyClaimModel()
          val refreshTokenClaim = emptyClaimModel.asJwtClaim(jwtConfig.refreshTokenTtlSeconds)
          val refreshTokenExpiry = refreshTokenClaim.expiration.getOrElse(0L)
          val refreshToken = getRefreshToken(emptyClaimModel)
  
          Right(UserLoginResponse(
            accessToken = accessToken,
            accessTokenExpiry = accessTokenExpiry,
            refreshToken = refreshToken,
            refreshTokenExpiry = refreshTokenExpiry,
          ))
    }
  }
}

object AuthenticationService {
  def of(jwtConfig: JwtConfig, userRepository: UserRepository): AuthenticationService = new AuthenticationService(jwtConfig, userRepository)
}

