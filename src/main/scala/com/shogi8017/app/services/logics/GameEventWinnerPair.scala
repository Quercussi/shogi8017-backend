package com.shogi8017.app.services.logics

import com.shogi8017.app.models.enumerators.GameWinner
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

case class GameEventWinnerPair(gameEvent: Option[GameEvent], winner: Option[GameWinner])

object GameEventWinnerPair {
  implicit val gameEventWinnerPairEncoder: Encoder[GameEventWinnerPair] = deriveEncoder
}