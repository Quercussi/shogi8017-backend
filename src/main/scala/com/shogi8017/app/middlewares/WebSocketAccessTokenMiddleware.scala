package com.shogi8017.app.middlewares

import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import com.shogi8017.app.JwtConfig
import com.shogi8017.app.middlewares.middlewareUtils.{AuthUserBuilder, CommonAuthHandlers, TokenDecoder}
import com.shogi8017.app.models.UserModel
import com.shogi8017.app.repository.TokenRepository
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

  private def authUser(jwtConfig: JwtConfig, tokenRepository: TokenRepository): Kleisli[IO, Request[IO], Either[String, UserModel]] = {

    val extractToken: Request[IO] => EitherT[IO, String, String] = { request =>
      def getTokenFromRequest: Either[String, String] =
        request.uri.query.params.get("websocketAccessToken").toRight("WebSocket access token not found in query parameters")

      def validateToken(token: String): IO[Either[String, String]] =
        tokenRepository.validateAndConsumeWebSocketToken(token).map {
          case true => Right(token)
          case false => Left("Invalid or expired WebSocket access token")
        }

      for {
        token <- EitherT.fromEither[IO](getTokenFromRequest)
        result <- EitherT(validateToken(token))
      } yield result
    }
      
    AuthUserBuilder.build(jwtConfig, extractToken, decodeToken)
  }

  def of(jwtConfig: JwtConfig, tokenRepository: TokenRepository): AuthMiddleware[IO, UserModel] = {
    AuthMiddleware(authUser(jwtConfig, tokenRepository), onFailure)
  }
}