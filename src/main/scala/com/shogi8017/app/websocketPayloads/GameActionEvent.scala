package com.shogi8017.app.websocketPayloads

import cats.effect.IO
import cats.syntax.functor.*
import com.shogi8017.app.routes.{PieceHandCount, PlayerList, PositionPiecePair}
import com.shogi8017.app.services.logics.{GameEventWinnerPair, Player, StateTransitionList}
import fs2.concurrent.Topic
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Encoder, Json}

import scala.collection.concurrent.TrieMap

/**
 * A trait that represents the body of all WebSocket requests via the `/ws/game` API.
 * All WebSocket requests that are part of the `/ws/game` API must extend this trait.
 */
sealed trait GameActionEvent extends WebSocketRequestBody

case class InvalidGameActionEvent(errorMessage: String) extends GameActionEvent

case class ExecutionActionEvent(stateTransitionList: StateTransitionList, gameEvent: GameEventWinnerPair) extends GameActionEvent

case class BoardConfigurationEvent(playerList: PlayerList, board: List[PositionPiecePair], handPieceCounts: List[PieceHandCount], currentPlayerTurn: Player) extends GameActionEvent

case object KeepAliveGameActionAPI extends GameActionEvent

object GameActionEvent {

  implicit val executionActionEventEncoder: Encoder[ExecutionActionEvent] = deriveEncoder
  
  implicit val boardConfigurationEventEncoder: Encoder[BoardConfigurationEvent] = deriveEncoder
  
  implicit val invalidGameActionEventEncoder: Encoder[InvalidGameActionEvent] = deriveEncoder

  implicit val gameActionEventEncoder: Encoder[GameActionEvent] = Encoder.instance {
    case board: BoardConfigurationEvent =>
      Json.obj(
        "type" -> Json.fromString("BoardConfiguration"),
        "event" -> boardConfigurationEventEncoder(board)
      )
    case executionActionEvent: ExecutionActionEvent =>
      Json.obj(
        "type" -> Json.fromString("ExecutionAction"),
        "event" -> executionActionEventEncoder(executionActionEvent)
      )
    case error: InvalidGameActionEvent =>
      Json.obj(
        "type" -> Json.fromString("InvalidGameAction"),
        "event" -> Json.obj(
          "errorMessage" -> invalidGameActionEventEncoder(error)
        )
      )
  }
}

type GameActionAPIRegistry =  TrieMap[String, Topic[IO, GameActionEvent]]

