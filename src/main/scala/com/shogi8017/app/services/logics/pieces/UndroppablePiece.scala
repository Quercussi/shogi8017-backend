package com.shogi8017.app.services.logics.pieces

import cats.data.Validated
import cats.data.Validated.Invalid
import com.shogi8017.app.errors.{ActionValidationError, IllegalDrop}
import com.shogi8017.app.services.logics.{Board, BoardTransition, DropAction}

trait UndroppablePiece extends Piece {
  def getBoardTransitionOnDrop(board: Board, drop: DropAction): Validated[ActionValidationError, BoardTransition] = Invalid(IllegalDrop)
}
