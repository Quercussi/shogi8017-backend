package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.{Board, Direction, Player, Position}

case class King(owner: Player) extends Piece with UnitMovingPieceMethods with UndroppablePiece {
  def pieceType: PieceType = {
    UnPromotablePieceType.KING
  }

  def score: Int = 0
  
  def unitDirections: List[Direction] = King.directions

  override def additionalOccupationValidation(board: Board, destination: Position): Boolean = {
    !destination.isUnderAttack(board, this.owner)
  }
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