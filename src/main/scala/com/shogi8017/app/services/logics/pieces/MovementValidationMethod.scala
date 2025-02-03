package com.shogi8017.app.services.logics.pieces

import cats.data.Validated
import com.shogi8017.app.errors.{ActionValidationError, CannotPromote, IllegalMove}
import com.shogi8017.app.services.logics.BoardAction.{ADD, HAND_ADD, REMOVE}
import com.shogi8017.app.services.logics.{Board, BoardTransition, MoveAction, Player, Position, StateTransition}

trait MovementValidationMethod extends Piece {
  def canMoveTo(board: Board, move: MoveAction): Boolean

  def getBoardTransitionOnMove(board: Board, move: MoveAction): Validated[ActionValidationError, BoardTransition] = {
    def validatePromotionAndApplyAction(piece: PromotablePiece, move: MoveAction): Validated[ActionValidationError, BoardTransition] = {
      piece.validatePromotion(board, move) match {
        case None => Validated.valid(getActionListOnMove(board, move))
        case Some(e) => Validated.invalid(e)
      }
    }

    if (!canMoveTo(board, move)) {
      Validated.invalid(IllegalMove)
    } else {
      (this, move.toPromote) match {
        case (p: PromotablePiece, _) =>
          validatePromotionAndApplyAction(p, move)
        case (_, true) => Validated.invalid(CannotPromote)
        case _ => Validated.valid(getActionListOnMove(board, move))
      }
    }
  }

  private def getActionListOnMove(board: Board, move: MoveAction): BoardTransition = {
    val (from, to) = move.getFromToPositions

    val fromAction: Option[StateTransition] = Some((REMOVE, from, owner, pieceType))

    val replacingPiece: PieceType = pieceType match {
      case promotable: PromotablePieceType if move.toPromote => PromotablePieceType.promote(promotable)
      case pt => pt
    }

    val toAction: Option[StateTransition] = Some((ADD, to, owner, replacingPiece))

    val capturingPiece = board.piecesMap.get(to)
    val capturingActions: List[Option[StateTransition]] = capturingPiece.map { p =>
      val pieceTypeToAddToHand = p.pieceType match {
        case promoted: PromotedPieceType => PromotedPieceType.demote(promoted)
        case other => other
      }
      List(
        Some((REMOVE, to, p.owner, p.pieceType)),
        Some((HAND_ADD, Position.sentinelPosition, Player.opponent(p.owner), pieceTypeToAddToHand))
      )
    }.getOrElse(List.empty)

    // NOTE: Ensure capturing actions are processed before movement actions
    val actions: List[Option[StateTransition]] = capturingActions ++ List(fromAction, toAction)
    val filteredActions: BoardTransition = (actions.flatten, "") // TODO: implement algebraic notation

    filteredActions
  }
}
