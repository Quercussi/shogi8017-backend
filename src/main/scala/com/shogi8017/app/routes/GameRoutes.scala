package com.shogi8017.app.routes

import cats.effect.IO
import com.shogi8017.app.models.{GameModel, UserModel}
import com.shogi8017.app.services.GameService
import com.shogi8017.app.services.logics.Board
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*

case class GameRoutes(gameService: GameService) {
  def getGameRoutes: AuthedRoutes[UserModel, IO] = AuthedRoutes.of[UserModel, IO] {
    case GET -> Root / "game" :? OffsetParam(offset) +& LimitParam(limit) as user =>
      val formattedOffset = offset.getOrElse(0)
      val defaultLimit = limit.getOrElse(10)
      val formattedLimit = if (defaultLimit > 50) 50 else defaultLimit
      
      for {
        response <- gameService.paginatedGetGameByUserId(PaginatedGetGameByUserIdPayload(user.userId, formattedOffset, formattedLimit)).value

        res <- response match {
          case Right(queryResponse) => Ok(queryResponse.asJson)
          case Left(error) => InternalServerError(s"Error: ${error.toString}")
        }
      } yield res

    case GET -> Root / "game" / gameCertificate / "history" :? OffsetParam(offset) +& LimitParam(limit) as user =>
      val formattedOffset = offset.getOrElse(0)
      val defaultLimit = limit.getOrElse(10)
      val formattedLimit = if (defaultLimit > 50) 50 else defaultLimit

      for {
        response <- gameService.paginatedGetGameHistory(PaginatedGetGameHistoryPayload(gameCertificate, formattedOffset, formattedLimit)).value

        res <- response match {
          case Right(queryResponse) => Ok(queryResponse.asJson)
          case Left(error) => InternalServerError(s"Error: ${error.toString}")
        }
      } yield res

    case GET -> Root / "game" / "defaultConfiguration" as user =>
      Ok(Board.convertToBoardConfiguration(Board.defaultInitialPosition).asJson)
  }
}

object GameRoutes {
  def of(gameService: GameService): GameRoutes = {
    GameRoutes(gameService)
  }
}