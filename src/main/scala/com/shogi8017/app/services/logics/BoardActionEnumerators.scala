package com.shogi8017.app.services.logics

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

enum BoardActionEnumerators:
  case REMOVE, ADD, HAND_ADD, HAND_REMOVE

object BoardActionEnumerators:
  implicit val boardActionEnumeratorsEncoder: Encoder[BoardActionEnumerators] = deriveEncoder