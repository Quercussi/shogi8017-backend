package com.chess8007.app.services.logics

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class PlayerAction(from: Position, to: Position, promoteTo: Option[PromotablePieceType] = None) {
  def getFromToPositions: (Position, Position) = (from, to)

  def getFields: (Position, Position, Option[PromotablePieceType]) = (from, to, promoteTo)
}

object PlayerAction {
  implicit val positionDecoder: Decoder[Position] = deriveDecoder[Position]
  implicit val promotablePieceDecoder: Decoder[PromotablePieceType] = deriveDecoder[PromotablePieceType]
  implicit val playerActionDecoder: Decoder[PlayerAction] = deriveDecoder[PlayerAction]
}