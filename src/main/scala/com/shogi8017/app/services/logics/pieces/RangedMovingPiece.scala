package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.Board.isOccupied
import com.shogi8017.app.services.logics.Direction.calculateDirection
import com.shogi8017.app.services.logics.{Board, Direction, MoveAction, Position}

import scala.annotation.tailrec

trait RangedMovingPiece extends Piece with UnitMovingPiece {
  protected def getAllRangedMovesInDirections(board: Board, position: Position, directions: List[Direction]): Set[Position] = {
    @tailrec
    def occupyDirection(position: Position, direction: Direction, acc: Set[Position] = Set.empty): Set[Position] = {
      val dest = position.move(direction)
      (canOccupy(board, position, direction), isCapture(board, position, direction)) match {
        case (true, false) => occupyDirection(dest, direction, acc + dest)
        case (true, true) => acc + dest
        case _ => acc
      }
    }

    directions.flatMap(direction => occupyDirection(position, direction)).toSet
  }

  protected def hasRangedMovesInDirections(board: Board, position: Position, directions: List[Direction]): Boolean = {
    hasUnitMovesInDirections(board, position, directions)
  }

  private def isSameDirection(board: Board, a: Direction, b: Direction): Boolean = {
    val (ai, aj, bi, bj) = (a.dx, a.dy, b.dx, b.dy)

    // it is the same direction when the dot product of
    // the two vectors is equal to the product of their magnitudes
    val prod = ai*bi + aj*bj
    val aMagSqr = ai*ai + aj*aj
    val bMagSqr = bi*bi + bj*bj
    prod > 0 && prod * prod == aMagSqr * bMagSqr
  }

  private def isCorrectDirection(board: Board, move: MoveAction, legalDirections: List[Direction]): Boolean = {
    val moveDirection = calculateDirection(move.from, move.to)
    legalDirections.exists(legalDirection => isSameDirection(board, moveDirection, legalDirection))
  }

  private def isPathClear(board: Board, move: MoveAction): Boolean = {
    val (from, to) = move.getFromToPositions
    val dx = to.x - from.x
    val dy = to.y - from.y

    // This unit of code works for straight line and diagonal lines.
    val steps = Math.max(Math.abs(dx), Math.abs(dy))
    val xDir = if (dx == 0) 0 else dx / Math.abs(dx)
    val yDir = if (dy == 0) 0 else dy / Math.abs(dy)

    // Check for intermediate positions excluding the destination
    val isPathClear = (1 until steps).forall { step =>
      val intermediatePosition = Position(from.x + step * xDir, from.y + step * yDir)
      !isOccupied(board, intermediatePosition)
    }

    // For the destination position, ensure it's either empty or an opponent's piece
    val destinationPiece = board.piecesMap.get(to)
    val isDestinationValid = !destinationPiece.exists(_.owner == this.owner)

    isPathClear && isDestinationValid
  }
  
  protected def canRangedMoveTo(board: Board, move: MoveAction, legalDirections: List[Direction]): Boolean = {
    isCorrectDirection(board, move, legalDirections) && isPathClear(board, move)
  }
}
