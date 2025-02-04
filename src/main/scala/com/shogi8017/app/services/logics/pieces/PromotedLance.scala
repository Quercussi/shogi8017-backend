package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.{Direction, Player}

case class PromotedLance(owner: Player) extends Piece with UnitMovingPieceMethods with UndroppablePiece {
  def pieceType: PieceType = {
    PromotedPieceType.P_LANCE
  }

  def unitDirections: List[Direction] = Gold.directions(this.owner)
}