package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.{Direction, Player}

case class Bishop(owner: Player) extends Piece with RangedMovingPieceMethods with DroppablePiece with PromotablePiece {
  def pieceType: PieceType = {
    PromotablePieceType.BISHOP
  }

  def score: Int = 5
  
  def rangedDirections: List[Direction] = Bishop.directions
}

object Bishop {
  val directions: List[Direction] = List(
    Direction(1, 1),
    Direction(1, -1),
    Direction(-1, 1),
    Direction(-1, -1),
  )
}
