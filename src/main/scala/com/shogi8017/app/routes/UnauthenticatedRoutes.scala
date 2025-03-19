package com.shogi8017.app.routes

import cats.data.EitherT
import cats.effect.IO
import com.shogi8017.app.services.UserService
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*

class UnauthenticatedRoutes(userService: UserService) {

  private def signUpUser(payload: UserSignUpPayload): EitherT[IO, Throwable, UserSignUpResponse] = {
    userService.signUpUser(payload).map(user => UserSignUpResponse(user.userId, user.username))
  }

  def getSignUpRoute: HttpRoutes[IO] = HttpRoutes.of {
    case req @ POST -> Root / "signUp" =>
      for {
        userSignUpPayload <- req.as[UserSignUpPayload]
        user <- userService.signUpUser(userSignUpPayload).value
        response <- user match {
          case Right(user) => Ok(user)
          case Left(error) => InternalServerError(s"Internal Server Error: ${error.toString}")
        }
      } yield response
  }
}

object UnauthenticatedRoutes {
  def of(userService: UserService): UnauthenticatedRoutes = new UnauthenticatedRoutes(userService)
}
