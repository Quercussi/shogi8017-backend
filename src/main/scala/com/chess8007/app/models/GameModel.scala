package com.chess8007.app.models

import com.chess8007.app.models.enumerators.{GameState, GameWinner}

case class GameModel(gameId: String, whiteUserId: String, blackUserId: String, winner: Option[GameWinner], status: GameState)
