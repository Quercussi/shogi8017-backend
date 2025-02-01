package com.shogi8017.app.services.logics.pieces

import cats.data.Validated
import com.shogi8017.app.errors.ActionValidationError
import com.shogi8017.app.services.logics.{Direction, Player}

case class Gold(owner: Player) extends Piece with UnitMovingPieceMethods with DroppablePiece {
  def pieceType: PieceType = {
    UnPromotablePieceType.GOLD
  }

  def unitDirections: List[Direction] = Gold.directions
}


object Gold {
  val directions: List[Direction] = List(
    Direction(1, 1),
    Direction(0, 1),
    Direction(-1, 1),
    Direction(1, 0),
    Direction(-1, 0),
    Direction(0, -1),
  )
}