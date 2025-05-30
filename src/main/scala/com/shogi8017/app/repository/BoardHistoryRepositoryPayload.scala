package com.shogi8017.app.repository

import com.shogi8017.app.models.BoardHistoryModel
import com.shogi8017.app.models.enumerators.ActionType
import com.shogi8017.app.services.logics.Player
import com.shogi8017.app.services.logics.pieces.PieceType

case class GetBoardHistoriesPayload(boardId: String)

case class GetBoardHistoriesPaginatedPayload(boardId: String, limit: Int)
case class GetBoardHistoriesPaginatedResponse(boardHistories: List[BoardHistoryModel],  nextOffset: Int, total: Int)

case class CreateBoardHistoryPayload(
  boardId: String,
  actionNumber: Int,
  actionType: ActionType,
  fromX: Option[Int] = None,
  fromY: Option[Int] = None,
  toX: Option[Int] = None,
  toY: Option[Int] = None,
  dropType: Option[PieceType] = None,
  toPromote: Boolean,
  player: Player
)