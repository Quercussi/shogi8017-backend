package com.shogi8017.app.models

import com.shogi8017.app.models.enumerators.ActionType
import com.shogi8017.app.services.logics.Player
import com.shogi8017.app.services.logics.pieces.PieceType

case class BoardHistoryModel(
  boardHistoryId: String,
  boardId: String,
  actionType: ActionType,
  actionNumber: Int,
  fromX: Option[Int],
  fromY: Option[Int],
  dropType: Option[PieceType],
  toX: Option[Int],
  toY: Option[Int],
  toPromote: Boolean,
  player: Player,
)