package com.shogi8017.app.services.logics.pieces

import cats.data.Validated
import com.shogi8017.app.exceptions.{ActionValidationException, IllegalMove}
import com.shogi8017.app.services.logics.pieces.Lance.directions
import com.shogi8017.app.services.logics.{Board, Direction, DropAction, Player}

case class Lance(owner: Player) extends Piece with RangedMovingPieceMethods with DroppablePiece with PromotablePiece {
  def pieceType: PieceType = {
    PromotablePieceType.LANCE
  }

  def rangedDirections: List[Direction] = Lance.directions(this.owner)

  override def additionalDropValidation(board: Board, drop: DropAction): Boolean = {
    drop.position.y != Lance.undroppableRank(this.owner)
  }

  override def forcedPromotionRanks: Option[List[Int]] = Some(Lance.forcedPromotionRanks(this.owner))
}

object Lance {
  def directions(player: Player): List[Direction] = List(
    if (player == Player.WHITE_PLAYER) Direction(0, 1) else Direction(0, -1)
  )

  private def undroppableRank(owner: Player): Int = {
    if (owner == Player.WHITE_PLAYER) 9 else 1
  }

  private def forcedPromotionRanks(owner: Player): List[Int] = {
    List(if (owner == Player.WHITE_PLAYER) 9 else 1)
  }
}