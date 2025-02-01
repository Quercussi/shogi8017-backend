package com.shogi8017.app.services.logics.pieces

import cats.data.Validated
import com.shogi8017.app.errors.{ActionValidationError, IllegalMove}
import com.shogi8017.app.services.logics.pieces.Lance.directions
import com.shogi8017.app.services.logics.utils.Multiset
import com.shogi8017.app.services.logics.{Board, Direction, DropAction, Player, Position}

case class Pawn(owner: Player) extends Piece with UnitMovingPieceMethods with DroppablePiece with PromotablePiece {
  def pieceType: PieceType = {
    PromotablePieceType.PAWN
  }

  def unitDirections: List[Direction] = Lance.directions(this.owner)

  override def additionalDropValidation(board: Board, drop: DropAction): Boolean = {
    lazy val pieceWithNoMoves = drop.position.y != Pawn.undroppableRank(this.owner)

    lazy val twoPawns = (1 to 9).exists(y =>
      board.piecesMap.get(drop.position).exists(p =>
        p.pieceType == PromotablePieceType.PAWN && p.owner == this.owner
      )
    )

    lazy val tempBoard = board.copy(
      piecesMap = board.piecesMap + (drop.position -> this),
      hands = board.hands.updated(this.owner, board.hands.getOrElse(this.owner, Multiset.empty) - this.pieceType),
      lastAction = board.lastAction
    )

    lazy val dropPawnMate = Board.isCheckmated(tempBoard, Player.opponent(this.owner))

    pieceWithNoMoves || twoPawns || dropPawnMate
  }
  
  override def forcedPromotionRank: Option[Int] = Some(Pawn.forcedPromotionRank(this.owner))
}

object Pawn {
  def directions(player: Player): List[Direction] = List(
    if (player == Player.WHITE_PLAYER) Direction(0, 1) else Direction(0, -1)
  )

  private def undroppableRank(owner: Player): Int = {
    if (owner == Player.WHITE_PLAYER) 9 else 1
  }
  
  private def forcedPromotionRank(owner: Player): Int = {
    if (owner == Player.WHITE_PLAYER) 9 else 1
  }
}