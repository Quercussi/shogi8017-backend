package com.shogi8017.app.services.logics.actions

import com.shogi8017.app.services.logics.pieces.PieceType
import com.shogi8017.app.services.logics.Position
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class DropAction(position: Position, pieceType: PieceType) extends OnBoardAction

object DropAction {
  implicit val positionDecoder: Decoder[Position] = deriveDecoder[Position]
  implicit val dropActionDecoder: Decoder[DropAction] = deriveDecoder[DropAction]
}