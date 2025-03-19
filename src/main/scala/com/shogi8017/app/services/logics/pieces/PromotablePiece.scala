package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.exceptions.{ExpectingPromotion, IllegalPromotion, IncorrectPromotionScenario}
import com.shogi8017.app.services.logics.actions.MoveAction
import com.shogi8017.app.services.logics.{Board, Player}

trait PromotablePiece extends Piece {
  def validatePromotion(board: Board, move: MoveAction): Option[IllegalPromotion] = {
    forcedPromotionRanks match {
      case Some(ranks) if ranks.contains(move.to.y) => Option.when(!move.toPromote)(ExpectingPromotion)

      case _ if !promotableRanks(this.owner).exists(r => r == move.from.y || r == move.to.y) && move.toPromote =>
        Some(IncorrectPromotionScenario)

      case _ => None
    }
  }


  protected def forcedPromotionRanks: Option[List[Int]] = None
  private def promotableRanks(player: Player): List[Int] = {
    if (player == Player.WHITE_PLAYER) List(7,8,9)
    else List(3,2,1)
  }
}