package com.shogi8017.app.services.logics.pieces

import cats.data.Validated
import com.shogi8017.app.errors.{ActionValidationError, IllegalDrop}
import com.shogi8017.app.services.logics.Board.{getEmptyPositions, isOccupied, isPlayerHandContains}
import com.shogi8017.app.services.logics.{Board, BoardAction, BoardTransition, DropAction, Position, StateTransitionList}
import com.shogi8017.app.services.logics.BoardAction.*

trait DroppablePiece extends Piece {
  def getBoardTransitionOnDrop(board: Board, drop: DropAction): Validated[ActionValidationError, BoardTransition] = {
    if(additionalDropValidation(board: Board, drop: DropAction)) {
      defaultGetBoardTransitionOnDrop(board, drop)
    } else {
      Validated.invalid(IllegalDrop)
    }
  }

  def getAllPossibleDrops(board: Board): Set[Position] = {
    getEmptyPositions(board).filter(additionalDropPositionFiltering(board, _))
  }
  
  private def defaultGetBoardTransitionOnDrop(board: Board, drop: DropAction): Validated[ActionValidationError, BoardTransition] = {
    if(isOccupied(board, drop.position) && isPlayerHandContains(board, this.owner, drop.pieceType)) {
      Validated.invalid(IllegalDrop)
    } else {
      val stateTransitionList: StateTransitionList = List(
        (ADD, drop.position, this.owner, this.pieceType),
        (HAND_REMOVE, Position.sentinelPosition,  this.owner, this.pieceType))
      // TODO: implement algebraic notation
      Validated.valid((stateTransitionList, ""))
    }
  }

  protected def additionalDropValidation(board: Board, drop: DropAction): Boolean = true

  private def additionalDropPositionFiltering(board: Board, position: Position): Boolean = {
    this.pieceType match {
      case promotablePieceType: PromotablePieceType => additionalDropValidation(board, DropAction(position, promotablePieceType))
      case _ => false
    }
  }
}
