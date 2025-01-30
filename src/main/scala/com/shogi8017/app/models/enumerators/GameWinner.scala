package com.shogi8017.app.models.enumerators

import com.shogi8017.app.services.logics.Player

enum GameWinner:
  case WHITE, BLACK, DRAW
  
object GameWinner {
  def toPlayer(gameWinner: GameWinner): Option[Player] = {
    gameWinner match {
      case GameWinner.WHITE => Some(Player.WHITE_PLAYER)
      case GameWinner.BLACK => Some(Player.BLACK_PLAYER)
      case _ => None
    }
  }
}