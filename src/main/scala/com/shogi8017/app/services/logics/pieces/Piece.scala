package com.shogi8017.app.services.logics.pieces

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import com.shogi8017.app.exceptions._
import com.shogi8017.app.services.logics._
import com.shogi8017.app.services.logics.Board._
import com.shogi8017.app.services.logics.actions.{DropAction, MoveAction, OnBoardAction}

trait Piece() {
  val owner: Player

  def score: Int
  
  def pieceType: PieceType

  def hasLegalMoves(board: Board, from: Position): Boolean

  def hasLegalDrop(board: Board): Boolean
  
  def getBoardTransitionOnMove(board: Board, move: MoveAction): Validated[ActionValidationException, BoardTransition]

  def getBoardTransitionOnDrop(board: Board, drop: DropAction): Validated[ActionValidationException, BoardTransition]

  def getAllPossibleMoves(board: Board, position: Position): Set[Position]

  private def isSelfPin(board: Board, move: MoveAction): Boolean = {
    val (from, to) = move.getFromToPositions
    
    val tempBoard = board.copy(
      piecesMap = board.piecesMap - from + (to -> board.piecesMap(from))
    )

    isChecked(tempBoard, this.owner)
  }

  protected def canOccupy(board: Board, from: Position, direction: Direction): Boolean = {
    val destination = from.move(direction)
    !destination.isOutOfBoard && board.piecesMap.get(destination).forall(_.owner != this.owner)
  }

  protected def isCapture(board: Board, position: Position, direction: Direction): Boolean = {
    val destination = position.move(direction)
    board.piecesMap.get(destination).exists(_.owner != owner)
  }
}

object Piece {
  def validateAndApplyAction(piece: Piece, board: Board, action: OnBoardAction): Validated[ActionValidationException, BoardStateTransition] = {
    val validationFunction: PartialFunction[OnBoardAction, Validated[ActionValidationException, BoardTransition]] = {
      case move: MoveAction if !piece.isSelfPin(board, move) =>
        piece.getBoardTransitionOnMove(board, move)
      case _: MoveAction =>
        Invalid(IllegalMove)
      case drop: DropAction =>
        piece.getBoardTransitionOnDrop(board, drop)
    }

    validationFunction(action).andThen(applyBoardTransition(board, piece.owner))
  }

  private def applyBoardTransition(board: Board, player: Player)(
    stateTransition: BoardTransition
  ): Validated[ActionValidationException, BoardStateTransition] = {
    val (transitions, algebraicNotation) = stateTransition
    val newBoard = processAction(board, player)(transitions)

    if (isChecked(newBoard, player)) Invalid(IllegalMove)
    else Valid((newBoard, transitions, algebraicNotation))
  }
}
