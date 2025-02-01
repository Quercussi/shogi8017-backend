package com.shogi8017.app.services.logics.pieces

import cats.data.Validated
import com.shogi8017.app.errors.ActionValidationError
import com.shogi8017.app.services.logics.{Direction, Player}

case class King(owner: Player) extends Piece with UnitMovingPieceMethods with UndroppablePiece {
  def pieceType: PieceType = {
    UnPromotablePieceType.KING
  }

  def unitDirections: List[Direction] = King.directions
}


object King {
  val directions: List[Direction] = List(
    Direction(1, 1),
    Direction(1, 0),
    Direction(1, -1),
    Direction(0, 1),
    Direction(0, -1),
    Direction(-1, 1),
    Direction(-1, 0),
    Direction(-1, -1)
  )
}