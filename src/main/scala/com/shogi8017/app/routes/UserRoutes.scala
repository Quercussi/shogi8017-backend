package com.shogi8017.app.routes

import cats.data.EitherT
import cats.effect.IO
import com.shogi8017.app.models.UserModel
import com.shogi8017.app.services.UserService
import io.circe.Encoder
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.{AuthedRoutes, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*

case class UserRoutes(userService: UserService) {

  private def handleResponse[A: Encoder](result: EitherT[IO, Throwable, A]): IO[Response[IO]] = {
    result.value.flatMap {
      case Right(data) => Ok(data.asJson)
      case Left(error) => InternalServerError(s"Error: ${error.toString}")
    }
  }

  def getUserRoutes: AuthedRoutes[UserModel, IO] = AuthedRoutes.of[UserModel, IO] {
    case GET -> Root / "user" / "search" :? SearchQueryParam(searchQuery) +& OffsetParam(offset) +& LimitParam(limit) +& ExcludeRequestingUserParam(excludeRequestingUser) as user =>
      val formattedSearchQuery = searchQuery
      val formattedOffset = offset.getOrElse(0)
      val formattedExcludeRequestingUser = excludeRequestingUser.getOrElse(true)
      val defaultLimit = limit.getOrElse(10)
      val formattedLimit = if (defaultLimit > 50) 50 else defaultLimit

      val excludingUserIds = if (formattedExcludeRequestingUser) List(user.userId) else List.empty

      handleResponse(
        userService.paginatedSearchUser(PaginatedSearchUserPayload(
          formattedSearchQuery, formattedOffset, formattedLimit, excludingUserIds
        ))
      )

    case GET -> Root / "user" / userId as user =>
      handleResponse(userService.getUserById(GetUserByIdPayload(userId)))
  }
}

object UserRoutes {
  def of(userService: UserService): UserRoutes = UserRoutes(userService)
}