package com.shogi8017.app.services.logics

import doobie.util.{Get, Put}
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

enum Player:
  case WHITE_PLAYER, BLACK_PLAYER

object Player:
  def opponent(player: Player): Player = player match {
    case WHITE_PLAYER => BLACK_PLAYER
    case BLACK_PLAYER => WHITE_PLAYER
  }
  
  implicit val playerGet: Get[Player] = Get[String].temap {
    case "WHITE_PLAYER" => Right(Player.WHITE_PLAYER)
    case "BLACK_PLAYER" => Right(Player.BLACK_PLAYER)
    case other => Left(s"Unknown player: $other")
  }
  
  implicit val playerPut: Put[Player] = Put[String].contramap(_.toString)
  
  implicit val playerEncoder: Encoder[Player] = deriveEncoder
