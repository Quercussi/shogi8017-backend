package com.shogi8017.app.repository

import cats.data.EitherT
import cats.effect.IO
import cats.effect.std.UUIDGen
import com.shogi8017.app.models.BoardHistoryModel
import doobie.*
import doobie.implicits.*

class BoardHistoryRepository(trx: Transactor[IO]) {

  def getBoardHistories(payload: GetBoardHistoriesPayload): EitherT[IO, Throwable, List[BoardHistoryModel]] = {
    EitherT {
      sql"""
        SELECT *
        FROM boardHistories
        WHERE boardId = ${payload.boardId}
        ORDER BY actionNumber
      """.query[BoardHistoryModel].to[List].transact(trx).attempt
    }
  }

  def createBoardHistory(payload: CreateBoardHistoryPayload): EitherT[IO, Throwable, BoardHistoryModel] = {
    for {
      boardHistoryUuid <- EitherT.liftF(UUIDGen[IO].randomUUID.map(_.toString))

      _ <- EitherT {
        sql"""
          INSERT INTO boardHistories (
            boardHistoryId, boardId, actionNumber, actionType,
            fromX, fromY, toX, toY, dropType, toPromote, player
          ) VALUES (
            $boardHistoryUuid, ${payload.boardId}, ${payload.actionNumber}, ${payload.actionType},
            ${payload.fromX}, ${payload.fromY}, ${payload.toX}, ${payload.toY},
            ${payload.dropType}, ${payload.toPromote}, ${payload.player}
          )
        """.update.run.transact(trx).attempt
      }

    } yield BoardHistoryModel(
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
    )
  }
}

object BoardHistoryRepository {
  def of(trx: Transactor[IO]): BoardHistoryRepository = new BoardHistoryRepository(trx)
}