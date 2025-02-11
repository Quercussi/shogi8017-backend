package com.shogi8017.app.services.logics.actions

import com.shogi8017.app.services.logics.Position
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class MoveAction(from: Position, to: Position, toPromote: Boolean = false) extends OnBoardAction {
  def getFromToPositions: (Position, Position) = (from, to)

  def getFields: (Position, Position, Boolean) = (from, to, toPromote)
}

object MoveAction {
  implicit val positionDecoder: Decoder[Position] = deriveDecoder[Position]
  implicit val playerActionDecoder: Decoder[MoveAction] = deriveDecoder[MoveAction]
}