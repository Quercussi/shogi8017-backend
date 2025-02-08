package com.shogi8017.app.services.logics.pieces

import cats.data.Validated
import com.shogi8017.app.exceptions.{IllegalMove, ActionValidationException}
import com.shogi8017.app.services.logics.{Board, BoardTransition, Direction, MoveAction, Position}

trait HybridMovingPieceMethod extends Piece with UnitMovingPiece with RangedMovingPiece with MovementValidationMethod {
  def unitDirections: List[Direction]
  def rangedDirections: List[Direction]

  def hasLegalMoves(board: Board, from: Position): Boolean = {
    hasRangedMovesInDirections(board, from, rangedDirections) ||
    hasUnitMovesInDirections(board, from, unitDirections)
  }

  override def canMoveTo(board: Board, move: MoveAction): Boolean =
    canUnitMoveTo(board, move, unitDirections) || canRangedMoveTo(board, move, rangedDirections)

  def getAllPossibleMoves(board: Board, position: Position): Set[Position] = {
    getAllUnitMovesInDirection(board, position, unitDirections) union
      getAllRangedMovesInDirections(board, position, rangedDirections)
  }
}
