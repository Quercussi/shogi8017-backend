package com.chess8007.app.routes

import cats.effect.IO
import cats.syntax.applicative.*
import com.chess8007.app.JwtConfig
import com.chess8007.app.database.DatabaseResource
import com.chess8007.app.errors.IncorrectUsernameOrPassword
import com.chess8007.app.services.AuthenticationService
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.{HttpRoutes, Response, ResponseCookie}

case class AuthenticationRoutes(authenticationService: AuthenticationService) {

  def getLoginRoute: HttpRoutes[IO] = HttpRoutes.of {
    case req @ POST -> Root / "api" / "login" =>
      for {
        userLoginPayload <- req.as[UserLoginPayload]
        response <- authenticationService.loginUser(userLoginPayload)
        res <- response match {
          case Right(userLoginResponse) => Ok(userLoginResponse.asJson)
            .map(_.addCookie(ResponseCookie("token", userLoginResponse.asJson.noSpaces)))
          case Left(IncorrectUsernameOrPassword) => Response[IO](status = Unauthorized)
            .withEntity("Incorrect username or password")
            .pure[IO]
          case Left(error) => InternalServerError(s"Error: ${error.toString}")
        }
      } yield res
  }

//  def getRefreshRoute[F[_]]: AuthedRoutes[TokenRefreshPayload, F] = AuthedRoutes.of[TokenRefreshPayload, F] {
//    case POST -> Root / "api" / "refresh" as refreshPayload => {
//      // TODO
//    }
//  }
}

object AuthenticationRoutes {
  def of(authenticationService: AuthenticationService) = new AuthenticationRoutes(authenticationService)
}
