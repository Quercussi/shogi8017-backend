package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.{Direction, Player}

case class PromotedRook(owner: Player) extends Piece with HybridMovingPieceMethod with UndroppablePiece {
  def pieceType: PieceType = {
    PromotedPieceType.P_ROOK
  }

  def score: Int = 5

  def unitDirections: List[Direction] = Bishop.directions
  def rangedDirections: List[Direction] = Rook.directions
}