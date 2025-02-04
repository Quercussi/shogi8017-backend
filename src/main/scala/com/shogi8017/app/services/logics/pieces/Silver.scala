package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.{Direction, Player}

case class Silver(owner: Player) extends Piece with UnitMovingPieceMethods with DroppablePiece with PromotablePiece {
  def pieceType: PieceType = {
    PromotablePieceType.SILVER
  }

  def unitDirections: List[Direction] = Silver.directions(this.owner)
}

object Silver {
  def directions(owner: Player): List[Direction] = {
    val dir = if (owner == Player.WHITE_PLAYER) 1 else -1
    List(
      Direction(1, 1*dir),
      Direction(0, 1*dir),
      Direction(-1, 1*dir),
      Direction(-1, -1*dir),
      Direction(1, -1*dir),
    )
  }
}