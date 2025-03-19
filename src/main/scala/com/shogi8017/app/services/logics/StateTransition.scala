package com.shogi8017.app.services.logics

import com.shogi8017.app.services.logics.pieces.PieceType
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

case class StateTransition(boardAction: BoardActionEnumerators, position: Position, player: Player, piece: PieceType)

object StateTransition {
  implicit val stateTransitionEncoder: Encoder[StateTransition] = deriveEncoder
}
