package com.chess8007.app.middlewares

import cats.effect.IO
import com.chess8007.app.JwtConfig
import com.chess8007.app.jwtClaimModels.UserClaimModel
import com.chess8007.app.models.UserModel
import dev.profunktor.auth.JwtAuthMiddleware
import org.http4s.server.AuthMiddleware

object AccessTokenMiddleware extends TokenMiddleware[UserClaimModel, UserModel] {
  override def classMapper: UserClaimModel => UserModel = { userClaimModel =>
    UserModel(
      userId = userClaimModel.userId,
      username = userClaimModel.username
    )
  }

  def of(jwtConfig: JwtConfig): AuthMiddleware[IO, UserModel] = {
    lazy val singletonAccessTokenMiddleware = JwtAuthMiddleware[IO, UserModel](jwtAuthAccess(jwtConfig.accessTokenSecret, jwtConfig.algorithm), authenticate)
    singletonAccessTokenMiddleware
  }
}
