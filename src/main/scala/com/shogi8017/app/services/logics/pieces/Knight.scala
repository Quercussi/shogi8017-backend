package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.{Board, Direction, DropAction, Player}

case class Knight(owner: Player) extends Piece with UnitMovingPieceMethods with DroppablePiece with PromotablePiece {
  def pieceType: PieceType = {
    PromotablePieceType.KNIGHT
  }

  def unitDirections: List[Direction] = Knight.directions(this.owner)

  override def additionalDropValidation(board: Board, drop: DropAction): Boolean = {
    !Knight.undroppableRanks(this.owner).contains(drop.position.y)
  }

  override def forcedPromotionRanks: Option[List[Int]] = Some(Knight.forcedPromotionRanks(this.owner))
}


object Knight {
  def directions(player: Player): List[Direction] = {
    val y_dir = if (player == Player.WHITE_PLAYER) 2 else -2
    List(
      Direction(1, y_dir),
      Direction(-1, y_dir),
    )
  }

  private def undroppableRanks(owner: Player): List[Int] = {
    if (owner == Player.WHITE_PLAYER) List(8,9) else List(2,1)
  }

  private def forcedPromotionRanks(owner: Player): List[Int] = {
    if (owner == Player.WHITE_PLAYER) List(8,9) else List(2,1)
  }
}