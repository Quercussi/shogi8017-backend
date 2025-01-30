package com.shogi8017.app.middlewares

import cats.effect.IO
import com.shogi8017.app.JwtConfig
import com.shogi8017.app.models.UserModel
import org.http4s.server.AuthMiddleware

case class MiddlewareCollection(accessTokenMiddleware: AuthMiddleware[IO, UserModel], refreshTokenMiddleware: AuthMiddleware[IO, UserModel])

object MiddlewareCollection {
  def instantiateMiddlewares(jwtConfig: JwtConfig): MiddlewareCollection = {
    val accessTokenMiddleware = AccessTokenMiddleware.of(jwtConfig)
    val refreshTokenMiddleware = RefreshTokenMiddleware.of(jwtConfig)
    MiddlewareCollection(
      accessTokenMiddleware = accessTokenMiddleware,
      refreshTokenMiddleware = refreshTokenMiddleware
    )
  }
}