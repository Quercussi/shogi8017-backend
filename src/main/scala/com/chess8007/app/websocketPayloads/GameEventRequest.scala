package com.chess8007.app.websocketPayloads

import cats.syntax.functor._
import com.chess8007.app.services.logics.PlayerAction
import io.circe.{Decoder, Json}
import io.circe.generic.semiauto.deriveDecoder

/**
 * A trait that represents the body of all WebSocket requests via the `/ws/game` API.
 * All WebSocket requests that are part of the `/ws/game` API must extend this trait.
 */
sealed trait GameEventRequest extends WebSocketRequestBody

/**
 * A case class representing a request to make a move in the game.
 *
 * @param move The player's move.
 */
case class MakeMoveRequest(move: PlayerAction) extends GameEventRequest

/**
 * A case class representing a request to resign from the game.
 */
case class ResignRequest() extends GameEventRequest

/**
 * A case class representing a request to draw the game.
 */
case class DrawRequest() extends GameEventRequest

object GameEventRequest {

  /**
   * A decoder for `GameEventRequest` that dispatches to the specific decoders based on the action field.
   */
  implicit val gameEventRequestDecoder: Decoder[GameEventRequest] = Decoder.instance { cursor =>
    WebSocketRequestBody.dispatchDecoder[GameEventRequest](
      cursor,
      {
        case "makeMove" => deriveDecoder[MakeMoveRequest].widen
        case "resign"   => deriveDecoder[ResignRequest].widen
        case "draw"     => deriveDecoder[DrawRequest].widen
      }
    )
  }

  def decoder: Decoder[GameEventRequest] = gameEventRequestDecoder
}
