package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.{Board, MoveAction, Player}

trait PromotablePiece extends Piece {
  def canPromote(board: Board, move: MoveAction): Boolean = {
    forcedPromotionRank match {
      case Some(rank) => rank == move.to.y || rank == move.from.y
      case None => promotableRanks(this.owner).contains(move.to.y)
    }
  }

  protected def forcedPromotionRank: Option[Int] = None
  private def promotableRanks(player: Player): List[Int] = {
    if (player == Player.WHITE_PLAYER) List(7,8,9)
    else List(3,2,1)
  }
}