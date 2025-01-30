package com.shogi8017.app.services.logics

import com.shogi8017.app.services.logics.{Piece, Position, PromotablePiece}

case class Move(player: Player, from: Position, to: Position, piece: Piece, promoteTo: Option[PromotablePiece] = None)
  extends PlayerAction(from, to, promoteTo) {
}