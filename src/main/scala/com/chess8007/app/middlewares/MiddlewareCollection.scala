package com.chess8007.app.middlewares

import cats.effect.IO
import com.chess8007.app.models.UserModel
import org.http4s.server.AuthMiddleware

case class MiddlewareCollection(accessTokenMiddleware: AuthMiddleware[IO, UserModel], refreshTokenMiddleware: AuthMiddleware[IO, UserModel])
