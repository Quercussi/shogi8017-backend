package com.shogi8017.app.services.logics

import com.shogi8017.app.services.logics.pieces.PieceType

case class DropAction(position: Position, pieceType: PieceType) extends PlayerAction
