package com.shogi8017.app.models.enumerators

import doobie.util.{Get, Put}
import io.circe.Encoder

enum GameState:
  case PENDING, ON_GOING, FINISHED


object GameState:
  implicit val gameStateGet: Get[GameState] = Get[String].temap {
    case "PENDING" => Right(GameState.PENDING)
    case "ON_GOING" => Right(GameState.ON_GOING)
    case "FINISHED" => Right(GameState.FINISHED)
    case other => Left(s"Unknown GameState: $other")
  }

  implicit val gameStateEncoder: Encoder[GameState] = Encoder.encodeString.contramap[GameState](_.toString)

  implicit val gameStatePut: Put[GameState] = Put[String].contramap(_.toString)