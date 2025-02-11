package com.shogi8017.app.services.logics

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

enum GameEvent:
  case CHECK, STALEMATE, DEAD_POSITION, CHECKMATE, RESIGNATION
  
object GameEvent:
  implicit val gameEventEncoder: Encoder[GameEvent] = deriveEncoder