package com.shogi8017.app.routes

import cats.effect.IO
import com.shogi8017.app.models.UserModel
import com.shogi8017.app.services.UserService
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*

case class UserRoutes(userService: UserService) {
  def getUserRoutes: AuthedRoutes[UserModel, IO] = AuthedRoutes.of[UserModel, IO] {
    case GET -> Root / "user" / "search" :? SearchQueryParam(searchQuery) +& OffsetParam(offset) +& LimitParam(limit) as user =>
      val formattedSearchQuery = searchQuery
      val formattedOffset = offset.getOrElse(0)
      val defaultLimit = limit.getOrElse(10)
      val formattedLimit = if (defaultLimit > 50) 50 else defaultLimit

      for {
        response <- userService.paginatedSearchUser(PaginatedSearchUserPayload(formattedSearchQuery, formattedOffset, formattedLimit))

        res <- response match {
          case Right(searchResponse) => Ok(searchResponse.asJson)
          case Left(error) => InternalServerError(s"Error: ${error.toString}")
        }
      } yield res
  }
}

object UserRoutes {
  def of(userService: UserService): UserRoutes = {
    UserRoutes(userService)
  }
}