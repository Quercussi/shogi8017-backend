package com.shogi8017.app.services.logics

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

enum GameEvent:
  case CHECK, STALEMATE, IMPASSE, CHECKMATE, RESIGNATION

object GameEvent:
  implicit val gameEventEncoder: Encoder[GameEvent] = Encoder.encodeString.contramap[GameEvent](_.toString)