package com.shogi8017.app.services.logics

case class Move(player: Player, from: Position, to: Position, piece: Piece, promoteTo: Option[PromotablePieceType] = None)