package com.shogi8017.app.middlewares

import cats.data.Kleisli
import cats.effect.IO
import com.shogi8017.app.JwtConfig
import com.shogi8017.app.middlewares.middlewareUtils.TokenDecoder.jwtAlgorithm
import com.shogi8017.app.middlewares.middlewareUtils.{AuthUserBuilder, CommonAuthHandlers, TokenDecoder}
import com.shogi8017.app.models.UserModel
import org.http4s.Request
import org.http4s.server.AuthMiddleware

object WebSocketAccessTokenMiddleware extends UserTokenMiddleware with CommonAuthHandlers {
  private def decodeToken(jwtConfig: JwtConfig, token: String): IO[Either[String, UserModel]] =
    TokenDecoder.decodeUserToken(
      token,
      jwtConfig.accessTokenSecret, // TODO: Assuming this should be access token secret
      jwtConfig.algorithm,
      classMapper,
      "Invalid WebSocket token payload",
      e => s"Invalid WebSocket token: $e"
    )

  private def authUser(jwtConfig: JwtConfig): Kleisli[IO, Request[IO], Either[String, UserModel]] = {
    val extractToken: Request[IO] => IO[Either[String, String]] = { request =>
      val k = request.uri.query.params.get("websocketAccessToken")
      IO.pure(
        request.uri.query.params.get("websocketAccessToken")
          .toRight("WebSocket access token not found in query parameters")
      )
    }

    AuthUserBuilder.build(jwtConfig, extractToken, decodeToken)
  }

  def of(jwtConfig: JwtConfig): AuthMiddleware[IO, UserModel] = {
    AuthMiddleware(authUser(jwtConfig), onFailure)
  }
}