package com.shogi8017.app.services.logics.pieces

import cats.data.Validated
import com.shogi8017.app.errors.{IllegalMove, ActionValidationError}
import com.shogi8017.app.services.logics.{Board, BoardTransition, Direction, MoveAction, Position}

trait UnitMovingPieceMethods extends UnitMovingPiece with MovementValidationMethod {
  def unitDirections: List[Direction]

  def hasLegalMoves(board: Board, from: Position): Boolean = {
    hasUnitMovesInDirections(board, from, unitDirections)
  }

  override def canMoveTo(board: Board, move: MoveAction): Boolean =
    canUnitMoveTo(board, move, unitDirections)

  def getAllPossibleMoves(board: Board, position: Position): Set[Position] = {
    getAllUnitMovesInDirection(board, position, unitDirections)
  }
}
