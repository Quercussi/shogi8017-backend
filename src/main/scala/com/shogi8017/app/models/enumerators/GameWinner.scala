package com.shogi8017.app.models.enumerators

import com.shogi8017.app.services.logics.Player
import doobie.util.{Get, Put}
import io.circe.Encoder

enum GameWinner:
  case WHITE_WINNER, BLACK_WINNER, DRAW
  
object GameWinner {
  def toPlayer(gameWinner: GameWinner): Option[Player] = {
    gameWinner match {
      case GameWinner.WHITE_WINNER => Some(Player.WHITE_PLAYER)
      case GameWinner.BLACK_WINNER => Some(Player.BLACK_PLAYER)
      case _ => None
    }
  }
  
  implicit val gameWinnerEncoder: Encoder[GameWinner] = Encoder.encodeString.contramap[GameWinner](_.toString)
  
  implicit val gameWinnerGet: Get[GameWinner] = Get[String].temap {
    case "WHITE_WINNER" => Right(GameWinner.WHITE_WINNER)
    case "BLACK_WINNER" => Right(GameWinner.BLACK_WINNER)
    case "DRAW" => Right(GameWinner.DRAW)
    case other => Left(s"Unknown GameWinner: $other")
  }
  
  implicit val gameWinnerPut: Put[GameWinner] = Put[String].contramap(_.toString)
}