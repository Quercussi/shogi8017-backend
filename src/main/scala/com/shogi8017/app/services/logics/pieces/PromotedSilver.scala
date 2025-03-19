package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.{Direction, Player}

case class PromotedSilver(owner: Player) extends Piece with UnitMovingPieceMethods with UndroppablePiece {
  def pieceType: PieceType = {
    PromotedPieceType.P_SILVER
  }

  def score: Int = 1

  def unitDirections: List[Direction] = Gold.directions(this.owner)
}