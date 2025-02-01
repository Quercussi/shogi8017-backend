package com.shogi8017.app.services.logics

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class MoveAction(from: Position, to: Position, toPromote: Boolean = false) extends PlayerAction {
  def getFromToPositions: (Position, Position) = (from, to)

  def getFields: (Position, Position, Boolean) = (from, to, toPromote)
}

object MoveAction {
  implicit val positionDecoder: Decoder[Position] = deriveDecoder[Position]
  implicit val playerActionDecoder: Decoder[MoveAction] = deriveDecoder[MoveAction]
}