package com.shogi8017.app.models

import com.shogi8017.app.models.enumerators.{GameState, GameWinner}

case class GameModel(
  gameId: String,
  gameCertificate: String,
  boardId: String,
  whiteUserId: String,
  blackUserId: String,
  winner: Option[GameWinner],
  gameState: GameState
)
