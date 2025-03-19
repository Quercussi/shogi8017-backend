package com.shogi8017.app.middlewares.middlewareUtils

import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import com.shogi8017.app.JwtConfig
import com.shogi8017.app.models.UserModel
import org.http4s.Request

object AuthUserBuilder {
  def build(
   jwtConfig: JwtConfig,
   extractToken: Request[IO] => EitherT[IO, String, String],
   decodeFunc: (JwtConfig, String) => EitherT[IO, String, UserModel]
  ): Kleisli[IO, Request[IO], Either[String, UserModel]] =
    Kleisli { request =>
      extractToken(request).flatMap {
        decodeFunc(jwtConfig, _)
      }.value
  }
}
