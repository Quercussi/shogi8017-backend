package com.shogi8017.app.services.logics

enum Player:
  case WHITE_PLAYER, BLACK_PLAYER
  
object Player:
  def opponent(player: Player): Player = player match {
    case WHITE_PLAYER => BLACK_PLAYER
    case BLACK_PLAYER => WHITE_PLAYER
  }