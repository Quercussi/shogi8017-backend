package com.shogi8017.app.services.logics.pieces

import cats.data.Validated
import com.shogi8017.app.errors.{IllegalMove, ActionValidationError}
import com.shogi8017.app.services.logics.pieces.Lance.directions
import com.shogi8017.app.services.logics.{Direction, Player}

case class Lance(owner: Player) extends Piece with RangedMovingPieceMethods with DroppablePiece with PromotablePiece {
  def pieceType: PieceType = {
    PromotablePieceType.LANCE
  }

  def rangedDirections: List[Direction] = Lance.directions(this.owner)
}

object Lance {
  def directions(player: Player): List[Direction] = List(
    if (player == Player.WHITE_PLAYER) Direction(0, 1) else Direction(0, -1)
  )
}