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

class InvitationRoutes(processQueue: Queue[IO, InvitationRequestContext], clientRegistry: InvitationAPIRegistry) {

  def getConnections(recipientsId: Set[String]): Set[Topic[IO, InvitationEvent]] = {
    recipientsId.flatMap(clientRegistry.get)
  }

  private def send(topic: Topic[IO, InvitationEvent]): Stream[IO, WebSocketFrame] =
    topic
      .subscribe(1000)
      .map{convertMessage}

  private def receive(user: UserModel, queue: Queue[IO, InvitationRequestContext])(wsfStream: Stream[IO, WebSocketFrame]): Stream[IO, Unit] = {
    wsfStream.evalMap {
      case Text(json, _) =>
        decode[InvitationRequest](json) match {
          case Right(request) =>
            queue.offer(WebSocketBodyContext(user, request))
          case Left(error) =>
            queue.offer(WebSocketBodyContext(user, InvalidInvitationBody(error.getMessage)))
        }

      case Close(_) =>
        queue.offer(WebSocketBodyContext(user, DisconnectInvitationAPI))

      case _ => IO.unit
    }
  }

  private def convertMessage(msg: InvitationEvent): WebSocketFrame = {
    msg match {
      case KeepAliveInvitationAPI => WebSocketFrame.Ping()
      case msg @ _   => WebSocketFrame.Text(msg.asJson.noSpaces)
    }
  }

  def routes(wsb: WebSocketBuilder2[IO]): AuthedRoutes[UserModel, IO] = AuthedRoutes.of {
    case GET -> Root / "invite_health" as user =>
      Ok(user.username)
    case GET -> Root / "invite" as user =>
      (for {
        t <- Topic[IO, InvitationEvent]
        _ <- IO.pure(clientRegistry.update(user.userId, t))
        inputPipe: Pipe[IO, WebSocketFrame, Unit] = receive(user, processQueue)
        ws <- wsb.build(send(t), inputPipe)
      } yield ws).handleErrorWith { err =>
        IO.println(s"Error handling WebSocket connection: ${err.getMessage}") *>
          IO.pure(Response[IO](Status.InternalServerError))
      }

  }
}

object InvitationRoutes {
  def initializeBuffer: Resource[IO, (Queue[IO, InvitationRequestContext], TrieMap[String, Topic[IO, InvitationEvent]])] = {
    for {
      queue <- Resource.eval(Queue.unbounded[IO, InvitationRequestContext])
      clientRegistry <- Resource.eval(IO(TrieMap.empty[String, Topic[IO, InvitationEvent]]))
    } yield (queue, clientRegistry)
  }

  def of(invitationRouteBuffer: InvitationRouteBuffer): InvitationRoutes = {
    InvitationRoutes(invitationRouteBuffer.processQueue, invitationRouteBuffer.clientRegistry)
  }
}