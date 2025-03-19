package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.*
import com.shogi8017.app.services.logics.actions.DropAction
import com.shogi8017.app.utils.Multiset

case class Pawn(owner: Player) extends Piece with UnitMovingPieceMethods with DroppablePiece with PromotablePiece {
  def pieceType: PieceType = {
    PromotablePieceType.PAWN
  }

  def score: Int = 1

  def unitDirections: List[Direction] = Lance.directions(this.owner)

  override def additionalDropValidation(board: Board, drop: DropAction): Boolean = {
    lazy val pieceWithNoMovesValidation = drop.position.y != Pawn.undroppableRank(this.owner)

    lazy val twoPawnsValidation = !(1 to 9).exists(y =>
      board.piecesMap.get(Position(drop.position.x, y)).exists(p =>
        p.pieceType == PromotablePieceType.PAWN && p.owner == this.owner
      )
    )

    lazy val tempBoard = board.copy(
      piecesMap = board.piecesMap + (drop.position -> this),
      hands = board.hands.updated(this.owner, board.hands.getOrElse(this.owner, Multiset.empty) - this.pieceType),
      currentPlayerTurn = Player.opponent(this.owner)
    )

    lazy val dropPawnMateValidation = !Board.isCheckmated(tempBoard, Player.opponent(this.owner))

    pieceWithNoMovesValidation && twoPawnsValidation && dropPawnMateValidation
  }
  
  override def forcedPromotionRanks: Option[List[Int]] = Some(Pawn.forcedPromotionRanks(this.owner))
}

object Pawn {
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