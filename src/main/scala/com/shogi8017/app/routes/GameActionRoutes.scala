package com.shogi8017.app.routes

import cats.effect.std.Queue
import cats.effect.{IO, Resource}
import com.shogi8017.app.models.UserModel
import com.shogi8017.app.websocketPayloads.*
import fs2.concurrent.Topic
import fs2.{Pipe, Stream}
import io.circe.parser.decode
import io.circe.syntax.*
import org.http4s.{AuthedRoutes, Response, Status}
import org.http4s.dsl.io.*
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.*

import scala.collection.concurrent.TrieMap

class GameActionRoutes(processQueue: Queue[IO, GameActionRequestContext], clientRegistry: GameActionAPIRegistry) {

  def getConnections(recipientsId: Set[String]): Set[Topic[IO, GameActionEvent]] = {
    recipientsId.flatMap(clientRegistry.get)
  }

  private def send(topic: Topic[IO, GameActionEvent]): Stream[IO, WebSocketFrame] =
    topic
      .subscribe(1000)
      .map{convertMessage}

  private def receive(gameUserPair: GameUserPair, queue: Queue[IO, GameActionRequestContext])(wsfStream: Stream[IO, WebSocketFrame]): Stream[IO, Unit] = {
    val (gameCertificate, user) = (gameUserPair.gameCertificate, gameUserPair.user)
    wsfStream.evalMap {
      case Text(json, _) =>
        decode[GameActionRequest](json) match {
          case Right(request) =>
            queue.offer(WebSocketBodyContext(gameUserPair, request))
          case Left(error) =>
            queue.offer(WebSocketBodyContext(gameUserPair, InvalidGameActionBody(error.getMessage)))
        }

      case Close(_) =>
        queue.offer(WebSocketBodyContext(gameUserPair, DisconnectGameActionAPI()))

      case _ => IO.unit
    }
  }

  private def convertMessage(msg: GameActionEvent): WebSocketFrame = {
    msg match {
      case KeepAliveGameActionAPI => WebSocketFrame.Ping()
      case msg @ _   => WebSocketFrame.Text(msg.asJson.noSpaces)
    }
  }

  def routes(wsb: WebSocketBuilder2[IO]): AuthedRoutes[UserModel, IO] = AuthedRoutes.of {
    case GET -> Root / "game" / gameCertificate as user =>
      (for {
        t <- Topic[IO, GameActionEvent]
        _ <- IO.pure(clientRegistry.update(user.userId, t))
        inputPipe: Pipe[IO, WebSocketFrame, Unit] = receive(GameUserPair(gameCertificate, user), processQueue)
        ws <- wsb.build(send(t), inputPipe)

        _ <- processQueue.offer(WebSocketBodyContext(GameUserPair(gameCertificate, user), ConnectGameActionAPI()))
      } yield ws).handleErrorWith { err =>
        IO.println(s"Error handling WebSocket connection: ${err.getMessage}") *>
        IO.pure(Response[IO](Status.InternalServerError))
      }
  }
}

object GameActionRoutes {
  def initializeBuffer: Resource[IO, (Queue[IO, GameActionRequestContext], TrieMap[String, Topic[IO, GameActionEvent]])] = {
    for {
      queue <- Resource.eval(Queue.unbounded[IO, GameActionRequestContext])
      clientRegistry <- Resource.eval(IO(TrieMap.empty[String, Topic[IO, GameActionEvent]]))
    } yield (queue, clientRegistry)
  }

  def of(gameActionRouteBuffer: GameActionRouteBuffer): GameActionRoutes = {
    GameActionRoutes(gameActionRouteBuffer.processQueue, gameActionRouteBuffer.clientRegistry)
  }
}