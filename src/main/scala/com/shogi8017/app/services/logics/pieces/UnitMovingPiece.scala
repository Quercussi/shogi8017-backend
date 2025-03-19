package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.actions.MoveAction
import com.shogi8017.app.services.logics.{Board, Direction, Position}

trait UnitMovingPiece extends Piece {
  
  protected def getAllUnitMovesInDirection(board: Board, position: Position, directions: List[Direction]): Set[Position] = {
    directions.foldLeft(Set.empty[Position]) { (acc, direction) =>
      val dest = position.move(direction)
      if (canOccupy(board, position, direction) && additionalOccupationValidation(board, position.move(direction))) acc + dest else acc
    }
  }

  protected def hasUnitMovesInDirections(board: Board, from: Position, directions: List[Direction]): Boolean = {
    directions.exists { direction =>
      val dest = from.move(direction)
      canOccupy(board, from, direction) && additionalOccupationValidation(board, from.move(direction))
    }
  }

  protected def canUnitMoveTo(board: Board, move: MoveAction, legalDirections: List[Direction]): Boolean = {
    val direction = Direction.calculateDirection(move.from, move.to)
    canOccupy(board, move.from, direction) && legalDirections.contains(direction)
  }

  protected def additionalOccupationValidation(board: Board, position: Position): Boolean = true
}
