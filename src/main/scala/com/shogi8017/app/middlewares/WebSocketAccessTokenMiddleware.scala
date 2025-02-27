package com.shogi8017.app.middlewares

import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import com.shogi8017.app.JwtConfig
import com.shogi8017.app.middlewares.middlewareUtils.{AuthUserBuilder, CommonAuthHandlers, TokenDecoder}
import com.shogi8017.app.models.UserModel
import org.http4s.Request
import org.http4s.server.AuthMiddleware

object WebSocketAccessTokenMiddleware extends UserTokenMiddleware with CommonAuthHandlers {
  private def decodeToken(jwtConfig: JwtConfig, token: String): EitherT[IO, String, UserModel] =
    TokenDecoder.decodeUserToken(
      token,
      jwtConfig.websocketAccessTokenSecret,
      jwtConfig.algorithm,
      classMapper,
      "Invalid WebSocket token payload",
      e => s"Invalid WebSocket token: $e"
    )

  private def authUser(jwtConfig: JwtConfig): Kleisli[IO, Request[IO], Either[String, UserModel]] = {
    val extractToken: Request[IO] => EitherT[IO, String, String] = { request =>
      val token = request.uri.query.params.get("websocketAccessToken")
      EitherT.fromEither[IO](token.toRight("WebSocket access token not found in query parameters"))
    }

    AuthUserBuilder.build(jwtConfig, extractToken, decodeToken)
  }

  def of(jwtConfig: JwtConfig): AuthMiddleware[IO, UserModel] = {
    AuthMiddleware(authUser(jwtConfig), onFailure)
  }
}