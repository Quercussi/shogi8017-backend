package com.shogi8017.app.repository

import cats.data.EitherT
import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.implicits.*
import com.shogi8017.app.models.GameModel
import com.shogi8017.app.models.enumerators.GameState.ON_GOING
import doobie.free.connection.*
import doobie.implicits.*
import doobie.util.query.Query0
import doobie.util.transactor.Transactor

import java.sql.Timestamp

class GameRepository(trx: Transactor[IO]) {

  def createGame(payload: CreateGamePayload): EitherT[IO, Throwable, GameModel] = {
    val program = for {
      boardId <- UUIDGen[ConnectionIO].randomUUID.map(_.toString)
      gameId  <- UUIDGen[ConnectionIO].randomUUID.map(_.toString)

      _ <-
        sql"""
            INSERT INTO boards (boardId)
            VALUES ($boardId)
          """.update.run

      _ <-
        sql"""
            INSERT INTO games (gameId, gameCertificate, boardId, whiteUserId, blackUserId, gameState)
            VALUES ($gameId, ${payload.gameCertificate}, $boardId, ${payload.whiteUserId}, ${payload.blackUserId}, $ON_GOING)
          """.update.run

      game <- sql"""
          SELECT *
          FROM games
          WHERE gameId = $gameId
        """.query[GameModel].unique
    } yield game

    EitherT(program.transact(trx).attempt)
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

  def paginatedGetGameByUserId(payload: PaginatedGetGameByUserIdPayloadRepo): EitherT[IO, Throwable, PaginatedGetGameByUserIdResponseRepo] = {
    val userId = payload.userId
    val offset = payload.offset
    val limit = payload.limit

    val gameQuery: Query0[GameModel] = {
      sql"""
        SELECT *
        FROM games
        WHERE whiteUserId = $userId OR blackUserId = $userId
        ORDER BY createdAt DESC
        LIMIT $limit OFFSET $offset
      """.query[GameModel]
    }

    val countQuery: Query0[Int] = {
      val baseQuery =
        sql"""
          SELECT COUNT(*) FROM games
          WHERE whiteUserId = $userId OR blackUserId = $userId
        """
      baseQuery.query[Int]
    }

    EitherT {
      (gameQuery.to[List], countQuery.unique).mapN { (games, total) =>
        val nextOffset = if (offset + limit < total) offset + limit else -1
        PaginatedGetGameByUserIdResponseRepo(games, nextOffset, total)
      }.transact(trx).attempt
    }
  }

  def patchGameState(payload: PatchGameStatePayload): EitherT[IO, Throwable, GameModel] = {
    val program = for {
      _ <-
        sql"""
          UPDATE games
          SET gameState = ${payload.gameState}
          WHERE gameId = ${payload.gameId}
        """.update.run

      game <- sql"""
          SELECT *
          FROM games
          WHERE gameId = ${payload.gameId}
        """.query[GameModel].unique
    } yield game

    EitherT(program.transact(trx).attempt)
  }
}

object GameRepository {
  def of(trx: Transactor[IO]): GameRepository = new GameRepository(trx)
}
