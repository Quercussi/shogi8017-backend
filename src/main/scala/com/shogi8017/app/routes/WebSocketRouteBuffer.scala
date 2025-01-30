package com.shogi8017.app.routes

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.std.Queue
import com.shogi8017.app.websocketPayloads.{InvitationAPIRegistry, InvitationRequestContext}

case class WebSocketRouteBuffer(invitationRouteBuffer: InvitationRouteBuffer)

object WebSocketRouteBuffer {
  def resource: Resource[IO, WebSocketRouteBuffer] = {
    for {
      (processQueue, clientRegistry) <- InvitationRoutes.initializeBuffer
    } yield WebSocketRouteBuffer(InvitationRouteBuffer(processQueue, clientRegistry))
  }
}

case class InvitationRouteBuffer(processQueue: Queue[IO, InvitationRequestContext], clientRegistry: InvitationAPIRegistry)
