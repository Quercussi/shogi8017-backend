package com.shogi8017.app.services.logics.pieces

import cats.data.Validated
import com.shogi8017.app.errors.ActionValidationError
import com.shogi8017.app.services.logics.{Board, Direction, DropAction, Player, Position}

case class Knight(owner: Player) extends Piece with UnitMovingPieceMethods with DroppablePiece with PromotablePiece {
  def pieceType: PieceType = {
    PromotablePieceType.KNIGHT
  }

  def unitDirections: List[Direction] = Knight.directions

  override def additionalDropValidation(board: Board, drop: DropAction): Boolean = {
    drop.position.y != Knight.undroppableRank(this.owner)
  }
  
  override def forcedPromotionRank: Option[Int] = Some(Knight.forcedPromotionRank(this.owner))
}


object Knight {
  val directions: List[Direction] = List(
    Direction(1, 2),
    Direction(-1, 2),
  )
  
  private def undroppableRank(owner: Player): Int = {
    if (owner == Player.WHITE_PLAYER) 8 else 2
  }

  private def forcedPromotionRank(owner: Player): Int = {
    if (owner == Player.WHITE_PLAYER) 8 else 2
  }
}