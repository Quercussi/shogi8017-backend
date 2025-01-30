package com.shogi8017.app.middlewares

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.syntax.applicative.*
import com.shogi8017.app.JwtConfig
import com.shogi8017.app.jwtClaimModels.UserClaimModel
import com.shogi8017.app.models.UserModel
import dev.profunktor.auth.jwt.{JwtAuth, JwtToken, jwtDecode}
import io.circe.Json
import io.circe.parser.decode
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, Request, Response, Status}

object RefreshTokenMiddleware extends TokenMiddleware[UserClaimModel, UserModel] {

  override def classMapper: UserClaimModel => UserModel = { userClaimModel =>
    UserModel(
      userId = userClaimModel.userId,
      username = userClaimModel.username
    )
  }

  private def decodeRefreshToken(jwtConfig: JwtConfig, token: String): IO[Either[String, UserModel]] = {
    val jwtAuth = JwtAuth.hmac(jwtConfig.refreshTokenSecret.toCharArray, jwtAlgorithm(jwtConfig.algorithm))
    jwtDecode[IO](JwtToken(token), jwtAuth).flatMap { claim =>
      decode[UserClaimModel](claim.content) match {
        case Right(userClaim) => IO.pure(Right(classMapper(userClaim)))
        case Left(_)          => IO.pure(Left("Invalid refresh token payload"))
      }
    }.handleError(e => Left(s"Invalid refresh token: ${e.toString}"))
  }

  private def onFailure: AuthedRoutes[String, IO] = {
    Kleisli { e =>
      OptionT.liftF {
        Response[IO](status = Status.Forbidden).withEntity(e.context).pure[IO]
      }
    }
  }

  private def authUser(jwtConfig: JwtConfig): Kleisli[IO, Request[IO], Either[String, UserModel]] =
    Kleisli { request =>
      request.as[Json].flatMap { json =>
        json.hcursor.get[String]("refreshToken") match {
          case Right(token) => decodeRefreshToken(jwtConfig, token)
          case Left(_)      => IO.pure(Left("Refresh token not found in request"))
        }
      }
    }

  def of(jwtConfig: JwtConfig): AuthMiddleware[IO, UserModel] = {
    AuthMiddleware(authUser(jwtConfig), onFailure)
  }
}
