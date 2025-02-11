package com.shogi8017.app.repository

import com.shogi8017.app.models.enumerators.ActionType
import com.shogi8017.app.services.logics.Player
import com.shogi8017.app.services.logics.pieces.PieceType

case class CreateGamePayload(gameCertificate: String, whiteUserId: String, blackUserId: String)

case class GetGamePayload(gameCertificate: String)

case class GetGameHistoriesPayload(gameId: String)

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