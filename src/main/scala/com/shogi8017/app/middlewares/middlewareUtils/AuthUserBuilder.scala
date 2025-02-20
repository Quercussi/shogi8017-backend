package com.shogi8017.app.middlewares.middlewareUtils

import cats.data.Kleisli
import cats.effect.IO
import com.shogi8017.app.JwtConfig
import com.shogi8017.app.models.UserModel
import org.http4s.Request

object AuthUserBuilder {
  def build(
   jwtConfig: JwtConfig,
   extractToken: Request[IO] => IO[Either[String, String]],
   decodeFunc: (JwtConfig, String) => IO[Either[String, UserModel]]
  ): Kleisli[IO, Request[IO], Either[String, UserModel]] = Kleisli { request =>
    extractToken(request).flatMap {
      case Right(token) => decodeFunc(jwtConfig, token)
      case Left(error)  => IO.pure(Left(error))
    }
  }
}
