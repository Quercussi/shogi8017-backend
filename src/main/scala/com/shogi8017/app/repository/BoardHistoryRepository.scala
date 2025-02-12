package com.shogi8017.app.repository

import cats.effect.IO
import cats.effect.std.UUIDGen
import com.shogi8017.app.models.BoardHistoryModel
import doobie.*
import doobie.implicits.*

class BoardHistoryRepository(trx: Transactor[IO]) {
  def getBoardHistories(payload: GetBoardHistoriesPayload): IO[Either[Throwable, List[BoardHistoryModel]]] = {
    (for {
      result <- (for {
        gameHistories <- sql"""
          SELECT *
          FROM boardHistories
          WHERE boardId = ${payload.gameId}
          ORDER BY actionNumber
        """.query[BoardHistoryModel].to[List]
      } yield gameHistories).transact(trx)
    } yield result).attempt
  }

  def createBoardHistory(payload: CreateBoardHistoryPayload): IO[Either[Throwable, BoardHistoryModel]] = {
    for {
      boardHistoryUuid <- UUIDGen[IO].randomUUID.map(_.toString)
      result <- sql"""
        INSERT INTO boardHistories (
          boardHistoryId, boardId, actionNumber, actionType,
          fromX, fromY, toX, toY, dropType, toPromote, player
        ) VALUES (
          $boardHistoryUuid, ${payload.boardId}, ${payload.actionNumber}, ${payload.actionType},
          ${payload.fromX}, ${payload.fromY}, ${payload.toX}, ${payload.toY},
          ${payload.dropType}, ${payload.toPromote}, ${payload.player}
        )
      """.update.run.transact(trx).attempt
    } yield result match {
      case Right(_) => Right(BoardHistoryModel(
        boardHistoryUuid,
        payload.boardId,
        payload.actionType,
        payload.actionNumber,
        payload.fromX,
        payload.fromY,
        payload.dropType,
        payload.toX,
        payload.toY,
        payload.toPromote,
        payload.player
      ))
      case Left(error) => Left(error)
    }
  }
}

object BoardHistoryRepository {
  def of(trx: Transactor[IO]): BoardHistoryRepository = new BoardHistoryRepository(trx)
}
