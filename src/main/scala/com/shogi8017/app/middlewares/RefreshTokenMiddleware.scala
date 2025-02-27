package com.shogi8017.app.middlewares

import cats.data.{EitherT, Kleisli}
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

  private def decodeRefreshTokenT(jwtConfig: JwtConfig, token: String): EitherT[IO, String, UserModel] =
    TokenDecoder.decodeUserToken(
      token,
      jwtConfig.refreshTokenSecret,
      jwtConfig.algorithm,
      classMapper,
      "Invalid refresh token payload",
      e => s"Invalid refresh token: $e"
    )

  private def extractTokenT(request: Request[IO]): EitherT[IO, String, String] =
    for {
      json  <- EitherT.liftF(request.as[Json])
      token <- EitherT.fromEither[IO](
        json.hcursor.get[String]("refreshToken").left.map(_ => "Refresh token not found in request")
      )
    } yield token

  private def authUser(jwtConfig: JwtConfig): Kleisli[IO, Request[IO], Either[String, UserModel]] =
    Kleisli { request =>
      (for {
        token <- extractTokenT(request)
        user  <- decodeRefreshTokenT(jwtConfig, token)
      } yield user).value
    }

  def of(jwtConfig: JwtConfig): AuthMiddleware[IO, UserModel] =
    AuthMiddleware(authUser(jwtConfig), onFailure)
}
