package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.{Direction, Player}

case class Rook(owner: Player) extends Piece with RangedMovingPieceMethods with DroppablePiece with PromotablePiece {
  def pieceType: PieceType = {
    PromotablePieceType.ROOK
  }

  def rangedDirections: List[Direction] = Rook.directions
}

object Rook {
  val directions: List[Direction] = List(
    Direction(1, 0),
    Direction(0, 1),
    Direction(-1, 0),
    Direction(0, -1),
  )
}
