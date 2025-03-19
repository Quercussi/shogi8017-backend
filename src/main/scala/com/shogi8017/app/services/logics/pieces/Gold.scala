package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.Player.WHITE_PLAYER
import com.shogi8017.app.services.logics.{Direction, Player}

case class Gold(owner: Player) extends Piece with UnitMovingPieceMethods with DroppablePiece {
  def pieceType: PieceType = {
    UnPromotablePieceType.GOLD
  }
  
  def score: Int = 1

  def unitDirections: List[Direction] = Gold.directions(this.owner)
}


object Gold {
  def directions(owner: Player): List[Direction] = {
    val dir = if owner == WHITE_PLAYER then 1 else -1
    List(
      Direction(1, 1*dir), Direction(0, 1*dir), Direction(-1, 1*dir),
      Direction(1, 0), Direction(-1, 0),
      Direction(0, -1*dir),
    )
  }
}