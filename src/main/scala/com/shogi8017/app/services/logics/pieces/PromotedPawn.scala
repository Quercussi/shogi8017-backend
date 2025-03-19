package com.shogi8017.app.services.logics.pieces

import cats.data.Validated
import com.shogi8017.app.exceptions.ActionValidationException
import com.shogi8017.app.services.logics.{Direction, Player}

case class PromotedPawn(owner: Player) extends Piece with UnitMovingPieceMethods with UndroppablePiece {
  def pieceType: PieceType = {
    PromotedPieceType.P_PAWN
  }

  def score: Int = 1

  def unitDirections: List[Direction] = Gold.directions(this.owner)
}