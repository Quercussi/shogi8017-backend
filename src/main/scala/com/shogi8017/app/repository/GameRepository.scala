package com.shogi8017.app.repository

import cats.data.EitherT
import cats.effect.IO
import cats.effect.std.UUIDGen
import com.shogi8017.app.models.enumerators.GameState.PENDING
import com.shogi8017.app.models.GameModel
import doobie.*
import doobie.implicits.*

class GameRepository(trx: Transactor[IO]) {

  def createGame(payload: CreateGamePayload): EitherT[IO, Throwable, GameModel] = {
    for {
      boardId <- EitherT.liftF(UUIDGen[IO].randomUUID.map(_.toString))
      gameId <- EitherT.liftF(UUIDGen[IO].randomUUID.map(_.toString))

      _ <- EitherT {
        sql"""
          INSERT INTO boards (boardId)
          VALUES ($boardId)
        """.update.run.transact(trx).attempt
      }

      _ <- EitherT {
        sql"""
          INSERT INTO games (gameId, gameCertificate, boardId, whiteUserId, blackUserId, gameState)
          VALUES ($gameId, ${payload.gameCertificate}, $boardId, ${payload.whiteUserId}, ${payload.blackUserId}, 'PENDING')
        """.update.run.transact(trx).attempt
      }

    } yield GameModel(gameId, payload.gameCertificate, boardId, payload.whiteUserId, payload.blackUserId, None, PENDING)
  }

  def getGame(payload: GetGamePayload): EitherT[IO, Throwable, Option[GameModel]] = {
    EitherT {
      sql"""
        SELECT *
        FROM games
        WHERE gameCertificate = ${payload.gameCertificate}
      """.query[GameModel].option.transact(trx).attempt
    }
  }
}

object GameRepository {
  def of(trx: Transactor[IO]): GameRepository = new GameRepository(trx)
}
