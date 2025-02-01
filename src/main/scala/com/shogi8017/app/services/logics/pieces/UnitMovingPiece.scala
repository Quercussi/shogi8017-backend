package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.{Board, Direction, MoveAction, Position}

trait UnitMovingPiece extends Piece {
  
  protected def getAllUnitMovesInDirection(board: Board, position: Position, directions: List[Direction]): Set[Position] = {
    directions.foldLeft(Set.empty[Position]) { (acc, direction) =>
      val dest = position.move(direction)
      if (canOccupy(board, position, direction)) acc + dest else acc
    }
  }

  protected def hasUnitMovesInDirections(board: Board, from: Position, directions: List[Direction]): Boolean = {
    directions.exists { direction =>
      val dest = from.move(direction)
      canOccupy(board, from, direction)
    }
  }

  protected def canUnitMoveTo(board: Board, move: MoveAction, legalDirections: List[Direction]): Boolean = {
    legalDirections.exists(direction => canOccupy(board, move.to, direction))
  }
}
