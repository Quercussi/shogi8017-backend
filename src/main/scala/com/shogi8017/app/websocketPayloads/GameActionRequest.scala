package com.shogi8017.app.websocketPayloads

import cats.syntax.functor.*
import com.shogi8017.app.services.logics.actions.{DropAction, MoveAction}
import io.circe.{Decoder, Json}
import io.circe.generic.semiauto.deriveDecoder

/**
 * A trait that represents the body of all WebSocket requests via the `/ws/game` API.
 * All WebSocket requests that are part of the `/ws/game` API must extend this trait.
 */
sealed trait GameActionRequest extends WebSocketRequestBody

case class MakeMoveRequest(move: MoveAction) extends GameActionRequest

case class MakeDropRequest(drop: DropAction) extends GameActionRequest

case class ResignRequest() extends GameActionRequest

case class InvalidGameActionBody(errorMessage: String) extends GameActionRequest

case class ConnectGameActionAPI() extends GameActionRequest

case class DisconnectGameActionAPI() extends GameActionRequest

object GameActionRequest {

  /**
   * A decoder for `GameActionRequest` that dispatches to the specific decoders based on the action field.
   */
  implicit val gameActionRequestDecoder: Decoder[GameActionRequest] = Decoder.instance { cursor =>
    WebSocketRequestBody.dispatchDecoder[GameActionRequest](
      cursor,
      {
        case "makeMove" => deriveDecoder[MakeMoveRequest].widen
        case "makeDrop" => deriveDecoder[MakeDropRequest].widen
        case "resign"   => deriveDecoder[ResignRequest].widen
      }
    )
  }

  def decoder: Decoder[GameActionRequest] = gameActionRequestDecoder
}