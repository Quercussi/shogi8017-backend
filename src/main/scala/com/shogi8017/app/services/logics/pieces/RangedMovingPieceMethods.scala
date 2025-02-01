package com.shogi8017.app.services.logics.pieces

import cats.data.Validated
import com.shogi8017.app.errors.{IllegalMove, ActionValidationError}
import com.shogi8017.app.services.logics.{Board, BoardTransition, Direction, MoveAction, Position}

trait RangedMovingPieceMethods extends RangedMovingPiece with MovementValidationMethod {
  def rangedDirections: List[Direction]

  def hasLegalMoves(board: Board, from: Position): Boolean = {
    hasRangedMovesInDirections(board, from, rangedDirections)
  }

  override def canMoveTo(board: Board, move: MoveAction): Boolean =
    canRangedMoveTo(board, move, rangedDirections)

  def getAllPossibleMoves(board: Board, position: Position): Set[Position] = {
    getAllRangedMovesInDirections(board, position, rangedDirections)
  }
}
