package com.shogi8017.app.services.logics

class PlayerAction(from: Position, to: Position, promoteTo: Option[PromotablePiece] = None) {
  def getFromToPositions: (Position, Position) = (from, to)

  def getFields: (Position, Position, Option[PromotablePiece]) = (from, to, promoteTo)
}
