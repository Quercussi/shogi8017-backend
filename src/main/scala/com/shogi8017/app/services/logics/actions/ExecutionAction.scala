package com.shogi8017.app.services.logics.actions

import cats.data.Validated
import com.shogi8017.app.exceptions.{InvalidBoardHistory, InvalidDropRecord, InvalidMoveRecord}
import com.shogi8017.app.models.BoardHistoryModel
import com.shogi8017.app.models.enumerators.ActionType.*
import com.shogi8017.app.services.logics.pieces.{PieceType, PromotablePieceType, UnPromotablePieceType}
import com.shogi8017.app.services.logics.*

case class ExecutionAction(player: Player, playerAction: PlayerAction)

object ExecutionAction {
  def convertToExecuteAction(boardHistory: BoardHistoryModel): Validated[InvalidBoardHistory, ExecutionAction] = {
    boardHistory.actionType match {
      case MOVE =>
        (boardHistory.fromX, boardHistory.fromY, boardHistory.toX, boardHistory.toY) match {
          case (Some(fromX), Some(fromY), Some(toX), Some(toY)) =>
            val from = Position(fromX, fromY)
            val to = Position(toX, toY)
            Validated.Valid(ExecutionAction(boardHistory.player, MoveAction(from, to, boardHistory.toPromote)))
          case _ =>
            Validated.Invalid(InvalidMoveRecord)
        }

      case DROP =>
        (boardHistory.dropType, boardHistory.toX, boardHistory.toY) match {
          case (Some(dropType), Some(toX), Some(toY)) =>
            val position = Position(toX, toY)
            Validated.Valid(ExecutionAction(boardHistory.player, DropAction(position, dropType)))
          case _ =>
            Validated.Invalid(InvalidDropRecord)
        }

      case RESIGN =>
        Validated.Valid (ExecutionAction(boardHistory.player, ResignAction()))
    }
  }
}
