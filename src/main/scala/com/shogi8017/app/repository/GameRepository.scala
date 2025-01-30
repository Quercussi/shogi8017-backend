package com.shogi8017.app.repository

import cats.effect.IO
import cats.effect.std.UUIDGen
import com.shogi8017.app.database.DatabaseResource
import com.shogi8017.app.models.enumerators.GameState.PENDING
import com.shogi8017.app.models.GameModel
import doobie.*
import doobie.implicits.*

class GameRepository(trx: Transactor[IO]) {
  def createGame(payload: CreateGamePayload): IO[Either[Throwable, GameModel]] = {
    (for {
      boardId <- UUIDGen[IO].randomUUID.map(_.toString)
      gameId <- UUIDGen[IO].randomUUID.map(_.toString)
      invitationId <- UUIDGen[IO].randomUUID.map(_.toString)
      result <- (for {
        _ <-
          sql"""
            INSERT INTO boards (boardId)
            VALUES ($boardId)
          """.update.run

        _ <-
          sql"""
            INSERT INTO games (gameId, boardId, whiteUserId, blackUserId, gameState)
            VALUES ($gameId, $boardId, ${payload.whiteUserId}, ${payload.blackUserId}, 'PENDING')
          """.update.run

        _ <-
          sql"""
            INSERT INTO invitations (invitationId, gameId)
            VALUES ($invitationId, $gameId)
          """.update.run

      } yield GameModel(gameId, payload.whiteUserId, payload.blackUserId, None, PENDING)).transact(trx)
    } yield result).attempt
  }
}

object GameRepository {
  def of(trx: Transactor[IO]): GameRepository = new GameRepository(trx)
}