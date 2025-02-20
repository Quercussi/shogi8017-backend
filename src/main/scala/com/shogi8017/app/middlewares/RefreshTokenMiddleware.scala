package com.shogi8017.app.middlewares

import cats.data.Kleisli
import cats.effect.IO
import com.shogi8017.app.JwtConfig
import com.shogi8017.app.jwtClaimModels.UserClaimModel
import com.shogi8017.app.middlewares.middlewareUtils.{AuthUserBuilder, CommonAuthHandlers, TokenDecoder}
import com.shogi8017.app.models.UserModel
import io.circe.Json
import org.http4s.Request
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.server.AuthMiddleware

object RefreshTokenMiddleware extends UserTokenMiddleware with CommonAuthHandlers {
  private def decodeRefreshToken(jwtConfig: JwtConfig, token: String): IO[Either[String, UserModel]] =
    TokenDecoder.decodeUserToken(
      token,
      jwtConfig.refreshTokenSecret,
      jwtConfig.algorithm,
      classMapper,
      "Invalid refresh token payload",
      e => s"Invalid refresh token: $e"
    )

  private def authUser(jwtConfig: JwtConfig): Kleisli[IO, Request[IO], Either[String, UserModel]] = {
    val extractToken: Request[IO] => IO[Either[String, String]] = { request =>
      request.as[Json].flatMap { json =>
        json.hcursor.get[String]("refreshToken") match {
          case Right(token) => IO.pure(Right(token))
          case Left(_)     => IO.pure(Left("Refresh token not found in request"))
        }
      }
    }

    AuthUserBuilder.build(jwtConfig, extractToken, decodeRefreshToken)
  }

  def of(jwtConfig: JwtConfig): AuthMiddleware[IO, UserModel] = {
    AuthMiddleware(authUser(jwtConfig), onFailure)
  }
}