package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.{Direction, Player}

case class PromotedBishop(owner: Player) extends Piece with HybridMovingPieceMethod with UndroppablePiece {
  def pieceType: PieceType = {
    PromotedPieceType.P_BISHOP
  }

  def unitDirections: List[Direction] = Rook.directions
  def rangedDirections: List[Direction] = Bishop.directions
}