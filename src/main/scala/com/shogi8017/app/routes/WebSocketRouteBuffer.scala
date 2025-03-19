package com.shogi8017.app.routes

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.std.Queue
import com.shogi8017.app.websocketPayloads.*

case class WebSocketRouteBuffer(invitationRouteBuffer: InvitationRouteBuffer, gameActionRouteBuffer: GameActionRouteBuffer)

object WebSocketRouteBuffer {
  def resource: Resource[IO, WebSocketRouteBuffer] = {
    for {
      (processQueue, clientRegistry) <- InvitationRoutes.initializeBuffer
      (processQueue2, clientRegistry2) <- GameActionRoutes.initializeBuffer
    } yield WebSocketRouteBuffer(
      InvitationRouteBuffer(processQueue, clientRegistry),
      GameActionRouteBuffer(processQueue2, clientRegistry2)
    )
  }
}

case class InvitationRouteBuffer(processQueue: Queue[IO, InvitationRequestContext], clientRegistry: InvitationAPIRegistry)

case class GameActionRouteBuffer(processQueue: Queue[IO, GameActionRequestContext], clientRegistry: GameActionAPIRegistry)