package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.{Direction, Player}

case class Silver(owner: Player) extends Piece with UnitMovingPieceMethods with DroppablePiece with PromotablePiece {
  def pieceType: PieceType = {
    PromotablePieceType.SILVER
  }

  def unitDirections: List[Direction] = Silver.directions
}

object Silver {
  val directions: List[Direction] = List(
    Direction(1, 1),
    Direction(0, 1),
    Direction(-1, 1),
    Direction(0, -1),
  )
}