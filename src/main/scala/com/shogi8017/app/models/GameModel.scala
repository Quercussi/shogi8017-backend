package com.shogi8017.app.models

import com.shogi8017.app.models.enumerators.{GameState, GameWinner}

case class GameModel(gameId: String, whiteUserId: String, blackUserId: String, winner: Option[GameWinner], status: GameState)
