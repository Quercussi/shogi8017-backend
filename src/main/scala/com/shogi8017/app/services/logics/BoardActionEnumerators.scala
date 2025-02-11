package com.shogi8017.app.services.logics

import io.circe.Encoder

enum BoardActionEnumerators:
  case REMOVE, ADD, HAND_ADD, HAND_REMOVE

object BoardActionEnumerators:
  implicit val boardActionEnumeratorsEncoder: Encoder[BoardActionEnumerators] = Encoder.encodeString.contramap[BoardActionEnumerators](_.toString)