package com.shogi8017.app.models.enumerators

sealed trait DropType
object DropType {
  case object ROOK extends DropType
  case object BISHOP extends DropType
  case object LANCE extends DropType
  case object KNIGHT extends DropType
  case object SILVER extends DropType
  case object GOLD extends DropType
  case object PAWN extends DropType
}

