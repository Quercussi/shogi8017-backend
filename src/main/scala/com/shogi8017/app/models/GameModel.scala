package com.shogi8017.app.models

import com.shogi8017.app.models.enumerators.{GameState, GameWinner}
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

import java.sql.Timestamp

case class GameModel(
  gameId: String,
  gameCertificate: String,
  boardId: String,
  whiteUserId: String,
  blackUserId: String,
  winner: Option[GameWinner],
  gameState: GameState,
  createdAt: Timestamp,
)

object GameModel {
  import TimeStampUtils.timestampEncoder
  
  implicit val gameModelEncoder: Encoder[GameModel] = deriveEncoder
}